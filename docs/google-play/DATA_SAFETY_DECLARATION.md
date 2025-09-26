# Google Play Data Safety Declaration - ForkSure

## Overview
This document provides the information needed to complete the Data Safety section in Google Play Console for ForkSure.

## Data Collection and Sharing

### Personal Information
**Does your app collect or share any of the following user data types?**

| Data Type                      | Collected | Shared | Purpose | Required/Optional |
|--------------------------------|-----------|--------|---------|-------------------|
| Name                           | ❌ No      | ❌ No   | N/A     | N/A               |
| Email address                  | ❌ No      | ❌ No   | N/A     | N/A               |
| User IDs                       | ❌ No      | ❌ No   | N/A     | N/A               |
| Address                        | ❌ No      | ❌ No   | N/A     | N/A               |
| Phone number                   | ❌ No      | ❌ No   | N/A     | N/A               |
| Race and ethnicity             | ❌ No      | ❌ No   | N/A     | N/A               |
| Political or religious beliefs | ❌ No      | ❌ No   | N/A     | N/A               |
| Sexual orientation             | ❌ No      | ❌ No   | N/A     | N/A               |
| Other personal info            | ❌ No      | ❌ No   | N/A     | N/A               |

### Financial Information
**Does your app collect or share any financial information?**

| Data Type            | Collected | Shared | Purpose | Required/Optional |
|----------------------|-----------|--------|---------|-------------------|
| User payment info    | ❌ No      | ❌ No   | N/A     | N/A               |
| Purchase history     | ❌ No      | ❌ No   | N/A     | N/A               |
| Credit score         | ❌ No      | ❌ No   | N/A     | N/A               |
| Other financial info | ❌ No      | ❌ No   | N/A     | N/A               |

### Health and Fitness
**Does your app collect or share any health and fitness information?**

| Data Type    | Collected | Shared | Purpose | Required/Optional |
|--------------|-----------|--------|---------|-------------------|
| Health info  | ❌ No      | ❌ No   | N/A     | N/A               |
| Fitness info | ❌ No      | ❌ No   | N/A     | N/A               |

### Messages
**Does your app collect or share any messages?**

| Data Type             | Collected | Shared | Purpose | Required/Optional |
|-----------------------|-----------|--------|---------|-------------------|
| Emails                | ❌ No      | ❌ No   | N/A     | N/A               |
| SMS or MMS            | ❌ No      | ❌ No   | N/A     | N/A               |
| Other in-app messages | ❌ No      | ❌ No   | N/A     | N/A               |

### Photos and Videos
**Does your app collect or share photos and videos?**

| Data Type | Collected | Shared | Purpose                                | Required/Optional |
|-----------|-----------|--------|----------------------------------------|-------------------|
| Photos    | ✅ Yes     | ✅ Yes  | App functionality - AI recipe analysis | Required          |
| Videos    | ❌ No      | ❌ No   | N/A                                    | N/A               |

**Details for Photos:**
- **Collection**: Photos are captured via camera for recipe analysis
- **Sharing**: Photos are sent to the Google Gemini AI service for processing
- **Storage**: Photos are NOT stored permanently - only held in memory during processing
- **User Control**: Users actively choose to take photos for analysis
- **Required**: Yes - core app functionality requires photo analysis

### Audio Files
**Does your app collect or share any audio files?**

| Data Type                 | Collected | Shared | Purpose | Required/Optional |
|---------------------------|-----------|--------|---------|-------------------|
| Voice or sound recordings | ❌ No      | ❌ No   | N/A     | N/A               |
| Music files               | ❌ No      | ❌ No   | N/A     | N/A               |
| Other audio files         | ❌ No      | ❌ No   | N/A     | N/A               |

### Files and Docs
**Does your app collect or share any files and docs?**

| Data Type      | Collected | Shared | Purpose | Required/Optional |
|----------------|-----------|--------|---------|-------------------|
| Files and docs | ❌ No      | ❌ No   | N/A     | N/A               |

### Calendar
**Does your app collect or share any calendar information?**

| Data Type       | Collected | Shared | Purpose | Required/Optional |
|-----------------|-----------|--------|---------|-------------------|
| Calendar events | ❌ No      | ❌ No   | N/A     | N/A               |

### Contacts
**Does your app collect or share any contact information?**

| Data Type | Collected | Shared | Purpose | Required/Optional |
|-----------|-----------|--------|---------|-------------------|
| Contacts  | ❌ No      | ❌ No   | N/A     | N/A               |

### App Activity
**Does your app collect or share any app activity information?**

| Data Type                    | Collected | Shared | Purpose | Required/Optional |
|------------------------------|-----------|--------|---------|-------------------|
| App interactions             | ❌ No      | ❌ No   | N/A     | N/A               |
| In-app search history        | ❌ No      | ❌ No   | N/A     | N/A               |
| Installed apps               | ❌ No      | ❌ No   | N/A     | N/A               |
| Other user-generated content | ❌ No      | ❌ No   | N/A     | N/A               |
| Other actions                | ❌ No      | ❌ No   | N/A     | N/A               |

### Web Browsing
**Does your app collect or share any web browsing information?**

| Data Type            | Collected | Shared | Purpose | Required/Optional |
|----------------------|-----------|--------|---------|-------------------|
| Web browsing history | ❌ No      | ❌ No   | N/A     | N/A               |

### App Info and Performance
**Does your app collect or share any app info and performance data?**

| Data Type                  | Collected | Shared | Purpose | Required/Optional |
|----------------------------|-----------|--------|---------|-------------------|
| Crash logs                 | ❌ No      | ❌ No   | N/A     | N/A               |
| Diagnostics                | ❌ No      | ❌ No   | N/A     | N/A               |
| Other app performance data | ❌ No      | ❌ No   | N/A     | N/A               |

### Device or Other IDs
**Does your app collect or share any device or other identifiers?**

| Data Type           | Collected | Shared | Purpose | Required/Optional |
|---------------------|-----------|--------|---------|-------------------|
| Device or other IDs | ❌ No      | ❌ No   | N/A     | N/A               |

## Data Security

### Encryption in Transit
**Is all user data encrypted in transit?**
✅ **Yes** - All API communications use HTTPS encryption

### Encryption at Rest
**Is all user data encrypted at rest?**
✅ **N/A** - No user data is stored at rest (photos are not saved)

### Data Deletion
**Do you provide a way for users to request that their data be deleted?**
✅ **N/A** - No user data is stored to delete (photos are automatically discarded after processing)

## Data Usage and Handling

### Photos Data Handling
- **Purpose**: Core app functionality - AI analysis of baked goods for recipe suggestions
- **Data Type**: Photos taken by the user via the camera
- **Collection Method**: User actively takes photos within the app
- **Sharing**: Sent to Google Gemini AI service for analysis
- **Storage**: NOT stored - only held in memory during processing
- **User Control**: Users choose when to take and analyze photos
- **Required**: Yes - essential for app functionality
- **Retention**: Immediate deletion after processing

## Third-Party Services

### Google Gemini AI
- **Service**: Google's Gemini AI for image analysis
- **Data Shared**: Photos taken by users
- **Purpose**: Recipe analysis and suggestions
- **Privacy Policy**: Governed by Google's Privacy Policy
- **Data Retention**: According to Google's policies (temporary processing)

## Summary for Google Play Console

**Key Points to Enter:**
1. **Photos are collected and shared** - for AI recipe analysis
2. **No personal information collected** - no names, emails, IDs, etc.
3. **Data encrypted in transit** - HTTPS for all communications
4. **No data stored permanently** - photos deleted after processing
5. **User control** - users actively choose to take photos
6. **Third-party sharing** - only with Google Gemini AI for core functionality
7. **No analytics or tracking** - no user behavior tracking

**Data Safety Summary:**
ForkSure is a privacy-focused app that only processes photos temporarily for AI recipe analysis. No personal information is collected, and no data is stored permanently on the device or servers. 