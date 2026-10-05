# Denarii Dolor Privacy Policy

Effective date: October 4, 2026

This policy explains how the Denarii Dolor app for Android ("the app") handles your information. The app is published by Joseph Anthony Abbott III ("I" or "me"). You can send questions about this policy to jabbottpublicsupport@gmail.com.

## In short

The app has no account, no server, no ads and no analytics, and it can't connect to the internet. What you enter stays on your phone, encrypted, unless you choose to export a report or a backup. I don't collect, receive, sell or share any of your information.

## What the app stores on your phone

- Your financial records: transactions, accounts, categories, budgets and balances. They are kept in a database encrypted with SQLCipher (256-bit AES). The database key is stored only in encrypted form, and only your PIN, your security answer or, if you turn it on, your fingerprint or face can unlock it.
- Your security profile: your security question, and the encrypted copies of the database key with the values needed to check your PIN and answer. Your PIN and your security answer themselves are never stored. The profile is encrypted with a key held in the Android Keystore.
- Sign-in protection: the number of failed sign-in attempts and the lockout timer.
- Settings: dark mode and your currency. These aren't encrypted, because they reveal nothing about your finances.

The app turns off Android's cloud backup and device-to-device transfer for all of its data, so none of it is copied to Google's servers or to another phone unless you export it yourself.

## What the app doesn't collect

The app doesn't collect or send your name, email address, phone number, contacts, location, photos, device identifiers, advertising ID or usage statistics. It contains no advertising, analytics or crash-reporting code, and it doesn't request the Internet permission, so it can't send anything anywhere.

## Permissions

- Biometric sign-in (USE_BIOMETRIC, and USE_FINGERPRINT on older Android versions): lets you sign in with a fingerprint or face if you turn it on. Android checks your biometrics; the app never sees or stores them.
- Hide overlay windows (HIDE_OVERLAY_WINDOWS): hides other apps' floating windows while the sign-in screen is shown, so they can't trick you into tapping something.

## Reports and backups you export

- Reports: a monthly report saved or shared as a CSV or PDF file contains that month's transactions and is not encrypted. It goes only where you choose: a folder on your phone, a cloud drive, or an app you pick to share it with, and that destination's own privacy policy then applies. Copies made for sharing are deleted from the app's storage when you sign out or your session ends, or else the next time the app starts.
- Backups: a backup file contains all your financial records and your currency setting, encrypted with a passphrase you choose. It doesn't contain your PIN, your security question or your biometric sign-in. It goes only where you choose. The backup can't be opened without the passphrase, and nobody, including me, can recover a forgotten passphrase. Choose a strong one: someone who gets a copy of the file could try to guess it.

## Google Play

If you get the app from Google Play, Google handles your purchase and download under its own privacy policy (https://policies.google.com/privacy). Google gives me information about sales, such as order numbers, prices and the country of purchase, and statistics such as install counts. If you have agreed to share usage and diagnostics data with Google, Google may also give me crash and performance reports for the app, which describe the error, the app version and the device model, not your financial records. I use this information only to fulfill orders, meet tax and legal obligations, and fix problems in the app.

## If you contact me

If you email me, I receive your email address and whatever you choose to include. I use them only to answer you, and I delete the conversation when it is no longer needed. Please don't send me your financial records, your PIN or a backup passphrase.

## Deleting your data

Your data is only on your phone, and in any reports or backups you exported. To delete it, use Wipe All App Data on the sign-in screen, or uninstall the app: either one removes the database and its keys from your phone. Delete exported reports and backups wherever you saved them. Because I don't hold any of your data, there is nothing for me to delete.

## Your rights

Depending on where you live, laws such as the EU General Data Protection Regulation or the California Consumer Privacy Act give you rights to access, correct, delete or move your personal information. Because the app keeps your information only on your phone, you can use these rights directly in the app: view and change your records, export them as reports or a backup, and delete them with Wipe All App Data. For anything else, email me.

## Children

The app is not directed to children under 13. It collects no personal information from anyone, children included.

## Security

Your data is encrypted on your phone as described above. The app asks for your PIN again after 5 minutes without use, and repeated wrong PINs or security answers lock sign-in for longer and longer. No protection is perfect: someone who knows or guesses your PIN, or who controls a compromised (rooted) phone, may be able to read your data.

## Changes to this policy

If this policy changes, the new version will be posted at the same address and included in the next app update, with a new effective date. Significant changes will also be described in the app's release notes.

## Contact

Joseph Anthony Abbott III, jabbottpublicsupport@gmail.com
