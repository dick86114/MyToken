#!/usr/bin/env bash
set -euo pipefail

if [[ ! -f "project.yml" ]]; then
  echo "错误：当前目录未找到 project.yml，请在仓库根目录运行此脚本。" >&2
  exit 1
fi

# 仅清理本脚本负责生成的两个目录。
rm -rf -- "build/dist" "build/DerivedData"
mkdir -p "build/dist"

xcodegen generate
xcodebuild \
  -project RoutinUsage.xcodeproj \
  -scheme RoutinUsage \
  -configuration Release \
  -derivedDataPath build/DerivedData \
  CODE_SIGNING_ALLOWED=NO \
  clean build

ditto \
  "build/DerivedData/Build/Products/Release/Routin Usage.app" \
  "build/dist/Routin Usage.app"

hdiutil create \
  -volname "Routin Usage" \
  -srcfolder "build/dist/Routin Usage.app" \
  -ov \
  -format UDZO \
  "build/dist/Routin-Usage.dmg"
