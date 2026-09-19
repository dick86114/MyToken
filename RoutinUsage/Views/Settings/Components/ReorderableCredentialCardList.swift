import SwiftUI

private let reorderableCardCoordinateSpace = "ReorderableCredentialCardList"

enum ReorderableCardGeometry {
    static func targetIndex(
        startIndex: Int,
        translation: CGFloat,
        step: CGFloat,
        count: Int
    ) -> Int {
        guard count > 0, step > 0 else { return 0 }
        let steps = Int((translation / step).rounded())
        return max(0, min(count - 1, startIndex + steps))
    }

    static func reorderedIDs<ID: Equatable>(
        _ ids: [ID],
        moving id: ID,
        to targetIndex: Int
    ) -> [ID] {
        guard let sourceIndex = ids.firstIndex(of: id) else { return ids }
        var updatedIDs = ids
        updatedIDs.remove(at: sourceIndex)
        let boundedIndex = max(0, min(targetIndex, updatedIDs.count))
        updatedIDs.insert(id, at: boundedIndex)
        return updatedIDs
    }

    static func offset(
        index: Int,
        startIndex: Int,
        targetIndex: Int,
        step: CGFloat,
        activeTranslation: CGFloat?
    ) -> CGFloat {
        if let activeTranslation {
            return activeTranslation
        }
        if startIndex < targetIndex, index > startIndex, index <= targetIndex {
            return -step
        }
        if startIndex > targetIndex, index >= targetIndex, index < startIndex {
            return step
        }
        return 0
    }
}

struct ReorderableCredentialCardList<ID: Hashable, Card: View>: View {
    let ids: [ID]
    let itemHeight: CGFloat
    let itemSpacing: CGFloat
    let move: (ID, Int) -> Bool
    @ViewBuilder let card: (ID) -> Card

    @State private var activeID: ID?
    @State private var startIndex: Int?
    @State private var currentIndex: Int?
    @State private var dragTranslation: CGFloat = .zero
    @State private var workingIDs: [ID] = []
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var step: CGFloat { itemHeight + itemSpacing }

    private var displayedIDs: [ID] {
        workingIDs.isEmpty ? ids : workingIDs
    }

    var body: some View {
        VStack(alignment: .leading, spacing: itemSpacing) {
            ForEach(displayedIDs, id: \.self) { id in
                card(id)
                    .frame(height: itemHeight)
                    .offset(y: dragOffset(for: id))
                    .transaction { transaction in
                        if activeID == id {
                            transaction.animation = nil
                        }
                    }
                    .scaleEffect(activeID == id ? 1.015 : 1)
                    .shadow(
                        color: .black.opacity(activeID == id ? 0.14 : 0),
                        radius: activeID == id ? 10 : 0,
                        y: activeID == id ? 4 : 0
                    )
                    .zIndex(activeID == id ? 10 : 0)
                    .contentShape(Rectangle())
                    .highPriorityGesture(dragGesture(for: id))
            }
        }
        .coordinateSpace(name: reorderableCardCoordinateSpace)
        .onAppear {
            workingIDs = ids
        }
        .onChange(of: ids) { _, updatedIDs in
            guard activeID == nil else { return }
            workingIDs = updatedIDs
        }
    }

    private func dragGesture(for id: ID) -> some Gesture {
        DragGesture(minimumDistance: 5, coordinateSpace: .named(reorderableCardCoordinateSpace))
            .onChanged { value in
                if activeID != id {
                    guard activeID == nil,
                          let index = displayedIDs.firstIndex(of: id)
                    else { return }

                    workingIDs = ids
                    activeID = id
                    startIndex = index
                    currentIndex = index
                }

                guard activeID == id else { return }
                dragTranslation = value.translation.height
                updateTarget(for: id)
            }
            .onEnded { _ in
                commitMove(for: id)
            }
    }

    private func updateTarget(for id: ID) {
        guard activeID == id,
              let startIndex,
              let currentIndex,
              !workingIDs.isEmpty
        else { return }

        let targetIndex = ReorderableCardGeometry.targetIndex(
            startIndex: startIndex,
            translation: dragTranslation,
            step: step,
            count: workingIDs.count
        )
        guard targetIndex != currentIndex else { return }

        withAnimation(
            reduceMotion
                ? nil
                : .interactiveSpring(response: 0.18, dampingFraction: 0.9)
        ) {
            self.currentIndex = targetIndex
        }
    }

    private func commitMove(for id: ID) {
        let destinationIndex = currentIndex
        let previewIDs = destinationIndex.map {
            ReorderableCardGeometry.reorderedIDs(workingIDs, moving: id, to: $0)
        } ?? workingIDs
        let didMove = if let startIndex,
                         let destinationIndex,
                         destinationIndex != startIndex {
            move(id, destinationIndex)
        } else {
            true
        }

        withAnimation(
            reduceMotion
                ? nil
                : .interactiveSpring(response: 0.2, dampingFraction: 0.92)
        ) {
            workingIDs = didMove ? previewIDs : ids
            activeID = nil
            startIndex = nil
            currentIndex = nil
            dragTranslation = .zero
        }
    }

    private func dragOffset(for id: ID) -> CGFloat {

        guard let startIndex,
              let currentIndex,
              let index = displayedIDs.firstIndex(of: id)
        else { return 0 }

        return ReorderableCardGeometry.offset(
            index: index,
            startIndex: startIndex,
            targetIndex: currentIndex,
            step: step,
            activeTranslation: activeID == id ? dragTranslation : nil
        )
    }
}
