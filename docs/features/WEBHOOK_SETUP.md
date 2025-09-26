# Webhook Setup for Silent Content Reporting

You must set up a webhook endpoint to enable silent content reporting without opening the email client. Here are several free options:

## Option 1: Make.com (Recommended)

1. **Sign up at [Make.com](https://www.make.com)** (free tier available)
2. **Create a new scenario**
3. **Add a Webhook trigger** (choose "Custom webhook")
4. **Copy the webhook URL** (looks like: `https://hook.eu2.make.com/abc123...`)
5. **Add an Email module** to forward reports to ravidor@gmail.com
6. **Replace the webhook URL** in `ContentReportingHelper.kt` line 76

## Option 2: Zapier

1. **Sign up at [Zapier](https://zapier.com)** (free tier available)
2. **Create a new Zap**
3. **Trigger: Webhook** - Choose "Catch Hook"
4. **Action: Email** - Send to ravidor@gmail.com
5. **Copy the webhook URL** and update `ContentReportingHelper.kt`

## Option 3: Formspree

1. **Sign up at [Formspree](https://formspree.io)** (free tier available)
2. **Create a new form** with endpoint like: `https://formspree.io/f/abc123`
3. **Configure to send emails** to ravidor@gmail.com
4. **Update the webhook URL** in `ContentReportingHelper.kt`

## Option 4: Simple Cloud Function

If you have a Google Cloud, AWS, or Vercel account:

```javascript
// Example Vercel function
export default async function handler(req, res) {
  if (req.method === 'POST') {
    const report = req.body;
    
    // Send email using your preferred service
    await sendEmail({
      to: 'ravidor@gmail.com',
      subject: `ForkSure Report: ${report.reason}`,
      body: JSON.stringify(report, null, 2)
    });
    
    res.status(200).json({ success: true });
  }
}
```

## Current Implementation

The app currently:
1. **Tries webhook first** (silent, no user interaction)
2. **Falls back to email** if webhook fails
3. **Logs all attempts** for debugging

## To Update the Webhook URL:

1. Open `app/src/main/java/com/ravidor/forksure/ContentReportingHelper.kt`
2. Find line 76: `val webhookUrl = "https://hook.eu2.make.com/your-webhook-id"`
3. Replace with your actual webhook URL
4. Rebuild the app

## Benefits of Webhook Approach:

✅ **Silent reporting** - No email client popup
✅ **Better user experience** - Seamless reporting
✅ **Structured data** - JSON format for easy processing
✅ **Automatic forwarding** - Reports sent directly to your email
✅ **Fallback support** - Email option if webhook fails
✅ **Google Play compliant** - Still in-app reporting

## JSON Report Format:

```json
{
  "app": "ForkSure",
  "timestamp": "2025-06-13 12:30:00",
  "reason": "Inappropriate content",
  "content": "The AI-generated recipe content...",
  "additional_details": "User's optional comments",
  "app_version": "1.1.4 (5)",
  "device_info": "Pixel 6 (Android 14)",
  "developer_email": "ravidor@gmail.com"
}
```

This provides a much better user experience while maintaining Google Play compliance!

