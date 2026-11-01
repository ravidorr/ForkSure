# Google Play Store Documentation

This folder contains all documentation related to Google Play Store compliance and listing.

## 📁 Contents

- **[GOOGLE_PLAY_STORE_LISTING.md](GOOGLE_PLAY_STORE_LISTING.md)** - Complete store listing content and guidelines
- **[DATA_SAFETY_DECLARATION.md](DATA_SAFETY_DECLARATION.md)** - Data safety section requirements and content
- **[GOOGLE_PLAY_COMPLIANCE_FIX.md](GOOGLE_PLAY_COMPLIANCE_FIX.md)** - AI content policy compliance implementation
- **[GOOGLE_PLAY_ASSETS_GUIDE.md](GOOGLE_PLAY_ASSETS_GUIDE.md)** - Asset creation and upload guidelines
- **[PLAY_STORE_LISTING.md](PLAY_STORE_LISTING.md)** - Basic store listing information

## 🎯 Overview

These documents ensure ForkSure complies with Google Play Store policies and requirements. They cover store listing content, data safety declarations, policy compliance, and asset guidelines for successful app publication. 

## 🤖 Automated Uploads from CI

The "Release on Merge" workflow uploads each new release AAB to Google Play. One-time setup:

1. Upload the first release manually in the Play Console (the Publishing API cannot create it) and keep Play App Signing enabled. The `ANDROID_KEYSTORE_*` secrets must hold the upload key Play expects.
2. In Google Cloud, enable the **Google Play Android Developer API**, create a service account, and download a JSON key.
3. In the Play Console, under Users and permissions, invite the service account email and grant it release access to ForkSure. Propagation can take a while.
4. Add the full JSON key as the repository secret `PLAY_SERVICE_ACCOUNT_JSON`. Never commit it.
5. Optional: set the repository variable `PLAY_TRACK` (for example `internal`) to override the default `production` track. This is a real release, not a dry run: the version counts as released afterwards and later runs skip the upload, so promote it in Play Console or bump `versionCode` to publish it elsewhere.

Release notes come from `distribution/whatsnew/whatsnew-en-US` (maximum 500 characters); update it with every release. If managed publishing is enabled in the Play Console, approved releases wait for a manual publish.
A failed upload fails the job but keeps the GitHub Release. Re-running does not upload again because the release exists; to retry, delete the GitHub Release and its tag, then re-run the workflow.
