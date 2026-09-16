import React, { useEffect, useState } from 'react';

export interface DownloadTarget {
  url: string;
  version: string;
}

export interface LatestDownloads {
  macos: DownloadTarget;
  android: DownloadTarget;
}

const REPOSITORY = 'dick86114/MyToken';
const RELEASES_URL = `https://github.com/${REPOSITORY}/releases`;
const RELEASES_API = `https://api.github.com/repos/${REPOSITORY}/releases?per_page=30`;
const CACHE_KEY = 'mytoken_latest_downloads_v1';
const CACHE_TTL = 15 * 60 * 1000;

const FALLBACK: LatestDownloads = {
  macos: { url: RELEASES_URL, version: '最新版' },
  android: { url: RELEASES_URL, version: '最新版' },
};

interface GitHubAsset {
  name: string;
  browser_download_url: string;
}

interface GitHubRelease {
  tag_name: string;
  draft: boolean;
  prerelease: boolean;
  assets: GitHubAsset[];
}

function parseVersion(tag: string): number[] | null {
  const match = tag.match(/(\d+)\.(\d+)\.(\d+)/);
  return match ? match.slice(1).map(Number) : null;
}

function compareVersion(left: number[], right: number[]): number {
  for (let index = 0; index < 3; index += 1) {
    if (left[index] !== right[index]) return left[index] - right[index];
  }
  return 0;
}

function pickLatest(releases: GitHubRelease[], matcher: RegExp): GitHubRelease | undefined {
  return releases
    .filter((release) => !release.draft && !release.prerelease && matcher.test(release.tag_name))
    .map((release) => ({ release, version: parseVersion(release.tag_name) }))
    .filter((item): item is { release: GitHubRelease; version: number[] } => item.version !== null)
    .sort((left, right) => compareVersion(right.version, left.version))[0]?.release;
}

function versionLabel(tag: string): string {
  const match = tag.match(/(\d+\.\d+\.\d+)/);
  return match ? `v${match[1]}` : '最新版';
}

function resolveMacTarget(release: GitHubRelease): DownloadTarget | null {
  const assets = release.assets.filter((asset) => asset.name.endsWith('.dmg'));
  const asset = assets.find((item) => item.name === 'MyToken.dmg')
    ?? assets.find((item) => /arm64|aarch64/i.test(item.name))
    ?? assets.find((item) => /x86_64|amd64/i.test(item.name))
    ?? assets[0];

  return asset ? { url: asset.browser_download_url, version: versionLabel(release.tag_name) } : null;
}

function resolveAndroidTarget(release: GitHubRelease): DownloadTarget | null {
  const assets = release.assets.filter((asset) => asset.name.endsWith('.apk'));
  const asset = assets.find((item) => /-android\.apk$/i.test(item.name))
    ?? assets.find((item) => !/debug/i.test(item.name))
    ?? assets[0];

  return asset ? { url: asset.browser_download_url, version: versionLabel(release.tag_name) } : null;
}

function readCache(): LatestDownloads | null {
  try {
    const cached = JSON.parse(localStorage.getItem(CACHE_KEY) ?? 'null') as {
      fetchedAt?: number;
      result?: LatestDownloads;
    } | null;
    if (cached?.result && cached.fetchedAt && Date.now() - cached.fetchedAt < CACHE_TTL) {
      return cached.result;
    }
  } catch {
    // 隐私模式或本地存储不可用时直接走网络请求。
  }
  return null;
}

function writeCache(result: LatestDownloads) {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify({ fetchedAt: Date.now(), result }));
  } catch {
    // 缓存失败不影响本次下载入口更新。
  }
}

async function resolveLatestDownloads(): Promise<LatestDownloads> {
  const response = await fetch(RELEASES_API, {
    headers: { Accept: 'application/vnd.github+json' },
  });
  if (!response.ok) throw new Error(`GitHub Releases API ${response.status}`);

  const releases = (await response.json()) as GitHubRelease[];
  const macRelease = pickLatest(releases, /^(macos-)?v\d+\.\d+\.\d+$/);
  const androidRelease = pickLatest(releases, /^android-v\d+\.\d+\.\d+$/);

  return {
    macos: macRelease ? resolveMacTarget(macRelease) ?? FALLBACK.macos : FALLBACK.macos,
    android: androidRelease ? resolveAndroidTarget(androidRelease) ?? FALLBACK.android : FALLBACK.android,
  };
}

let currentDownloads: LatestDownloads = readCache() ?? FALLBACK;
let loadPromise: Promise<LatestDownloads> | null = null;
const listeners = new Set<React.Dispatch<React.SetStateAction<LatestDownloads>>>();

function publish(result: LatestDownloads) {
  currentDownloads = result;
  listeners.forEach((listener) => listener(result));
}

function ensureLatestDownloadsLoaded() {
  if (!loadPromise) {
    loadPromise = resolveLatestDownloads()
      .then((result) => {
        writeCache(result);
        publish(result);
        return result;
      })
      .catch(() => {
        publish(FALLBACK);
        return FALLBACK;
      });
  }
  return loadPromise;
}

export function useLatestDownloads(): LatestDownloads {
  const [downloads, setDownloads] = useState<LatestDownloads>(currentDownloads);

  useEffect(() => {
    listeners.add(setDownloads);
    setDownloads(currentDownloads);
    void ensureLatestDownloadsLoaded();

    return () => {
      listeners.delete(setDownloads);
    };
  }, []);

  return downloads;
}
