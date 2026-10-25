# Website and GitHub Pages

The product website lives in `site/`. It uses HTML, CSS and an optional local WebGL cloud shader, with project-owned artwork and sample screenshots. It has no package-manager dependencies, analytics, external fonts or required account.

The [website design brief](../site/DESIGN.md) defines the visual system and acceptance criteria. It is separate from the Android app's design rules.

## Preview locally

From the repository folder:

```sh
python3 scripts/build-site.py
python3 scripts/check-site.py
python3 -m http.server 8765 --directory .site-build
```

Open `http://localhost:8765`. Stop the server with Ctrl+C.

The website presents Home and local profiles using curated app screenshots, with a procedural cloud introduction, remote-navigation explanations, privacy details and native FAQ disclosures. The decorative sky enhancement pauses offscreen and when the page is hidden, and renders a still frame for reduced motion. CSS provides a fallback without JavaScript or WebGL. A separate small script lights the decorative footer wordmark under a fine pointer; it stays static with reduced motion. Header controls use simple CSS frost with fixed hover geometry. The visual-quality comparison section remains removed.

## Activate GitHub Pages

1. Push the reviewed `main` branch to `git@github.com:Deuspheara/oree.git` when ready.
2. On GitHub, open **Settings → Pages → Build and deployment** and select **GitHub Actions** as the source.
3. Open **Actions → Website → Run workflow**, using `main`. Every push to `main` deploys the website automatically.
4. Open the URL shown by the deployment. With the standard project domain, it will be `https://deuspheara.github.io/oree/`.

No SSH key or deploy secret belongs in the workflow. Your GitHub SSH setup is used only for Git operations. Pages deployment uses GitHub's short-lived workflow credentials. The workflow does not activate Pages in repository settings for you.

## What CI does

- **Android CI:** runs for Android/build changes and manual dispatch. Separate steps check assets, formatting, architecture, unit tests, lint and all required APK compilation. Reports and development APKs are kept for seven days. PRs use read-only Gradle caches; superseded runs are cancelled.
- **Website:** builds and checks the site on every `main` push and relevant PRs. Only the default branch can deploy, through the `github-pages` environment. PRs never deploy. Site-only edits do not trigger a full Android build.
- Actions are pinned to immutable commits. Dependabot groups monthly Action updates into reviewable PRs.

The build copies an explicit public-file allowlist into the ignored `.site-build/` folder. It never publishes the repository root, private evidence, configuration, signing material or APKs. Local links are relative so the site works under a project path as well as a custom domain.

See [GitHub's custom Pages workflow guide](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages) for hosting details. Passing local checks does not establish that a hosted workflow has run successfully.
