# FairShare — Shared Expense Tracker for College Students

**Team:** Maral Bat-Erdene, Aurelia Peterson Rajalingam, Jacqueline Guzman Nunez

**Demo:** [▶ Watch Demo](https://youtu.be/F8LlJvuENdw)
---

Our project idea is creating a mobile budgeting app designed for college students to easily manage and split shared expenses with friends. Users can create private rooms for different groups, such as “Dorm Room,” “Trip to Vienna,” or “Club Event,” where everyone has a personal profile. Members can post shared costs, attach receipt photos, and choose how to split expenses. There will be options to split equally, by selected items, or custom amounts. Users can tick off payments as they are made, and the app will automatically update the balance to show who has paid and who still owes.

As students post their expenses, the app automatically calculates the total balance for each person, displaying clear plus and minus tags to indicate who should receive or send money. It then provides the final summarized amount each person needs to pay or receive, helping users include this information as the payment description when sending money.
To keep students organized, the app sends payment reminders for upcoming or overdue amounts. It can also convert currencies in real time, which is useful for international students or study-abroad trips. Users can view visual summaries of spending, payments, and outstanding balances through charts and statistics. Additional features include the ability to edit or comment on expenses, add tags or notes for clarity, such as “pizza night” or “utilities,” and manage recurring costs such as rent or shared subscriptions. The app aims to make group budgeting and expense tracking easy, fair, and visually engaging for college life.

Users will have to create an account when they start to use the app. When creating an account, they will be asked for their name, email, phone number, and all of the financial apps they can send and receive money through (for example: Zelle, PayPal, Venmo, CashApp, etc.). When listing all of the financial apps they have accounts with, they will also have to provide information like their username on the financial app, which then other users can use this information to easily send them money through. This will make things easier later on, as when splitting a bill with friends, the user’s friends can click on the user’s profile picture and a small pop up will appear with the financial apps the user accepts and what information the friends need to know so that the friends won’t have to ask the user 50 times for their phone number or username on the financial app. 
Below we have included an image of what we believe the app would look like and behave like. Please let us know if this is too complicated or we dismissed a vital detail. Also, advice on how to gather and retrieve the info needed for this project would be greatly appreciated. 

### Features

- **Expense Rooms** — Create named groups (e.g. "Vienna Trip", "Apartment 4B") and invite friends via email
- **Bill Splitting** — Split equally across all members or assign individual items to specific people
- **Receipt Scanning** — Photograph a receipt and let AI automatically extract and assign line items
- **Real-Time Balances** — Firestore-backed live sync shows who owes what the moment a bill is added
- **Multi-Currency Support** — Live exchange rates via Fixer.io for international students and study-abroad trips
- **Settle Up Flow** — Record payments via Cash, Venmo, Zelle, PayPal, or bank transfer with balance auto-update
- **Invitation System** — Pending invite flow ensures users consent before joining a group
- **Archived Rooms** — Close out settled groups while keeping a read-only history

### Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose, Material3 |
| Auth & Database | Firebase Auth, Firestore |
| Image Storage | Supabase Storage |
| AI Receipt Scanning | Anthropic Claude API |
| Exchange Rates | Fixer.io |
| Image Loading | Coil |
| Dependency Injection | Hilt |
| Navigation | Navigation3 |

### Architecture

MVVM with Firestore real-time snapshot listeners. All balance calculations are handled server-side via Firestore transactions to prevent race conditions. Currency amounts are stored internally in EUR and converted to the user's preferred currency at display time.
