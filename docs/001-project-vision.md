# 001 — Project Vision

**Date:** 2026-03-06
**Status:** Active

## Problem Statement

Existing Markdown editor plugins for PHPStorm are inadequate:
- Poor behavior and buggy implementations
- Poorly documented for English-speaking audiences
- Significantly behind VSCode's Markdown editing experience

PHPStorm deserves a Markdown editing experience that developers can be proud of.

## Vision

Build a comprehensive, free, open-source Markdown editing plugin for PHPStorm that matches or exceeds the quality of VSCode's Markdown All-in-One extension — purpose-built for the JetBrains/IntelliJ Platform.

## Goals

1. **Feature parity with VSCode** — keyboard shortcuts, smart list editing, TOC, table formatting, auto-completion, smart paste, HTML export
2. **Native IntelliJ feel** — use platform APIs, follow JetBrains conventions, integrate with the existing bundled Markdown plugin
3. **Quality first** — comprehensive tests, proper documentation, transparent development
4. **Community-ready** — designed for personal use initially, but architected for public release on the JetBrains Marketplace

## Audience

- **Primary:** The author (personal use in PHPStorm)
- **Secondary:** PHPStorm/IntelliJ users who work with Markdown regularly
- **Tertiary:** The broader JetBrains plugin community

## Principles

- Extend, don't replace — build on IntelliJ's bundled Markdown support
- Every feature is tested before it ships
- Documentation lives in the repo, always
- Development is phased and incremental
