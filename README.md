# ParkWizard

ParkWizard is an android mobile app that provides users with real-time street parking information. It also delivers a rough estimate on times when occupied parking spots would most likely be vacated. Users will have the ability to locate open parking spots on a map-based GUI. The information is provided by the app users themselves i.e the app’s design model follows a trust-based crowdsourcing approach. For this reason the success of this idea heavily relies on the assumption that our app would have a large user base.

Users create an account by signing up through Google or Facebook. When a user wants to find a parking spot, the app utilizes the user’s location information to display all the nearby available parking spots with green dots and occupied ones with red dots. As mentioned earlier, users supply parking information to the app which is displayed to other users seeking parking spots. This can be done in two ways. First, after occupying a parking spot, users can submit details about their location and expected vacating time. Second, even users who aren’t looking for parking but witness open parking spaces can submit such details to the app. This is useful as a larger real-time dataset can cater more number of customers.

But the question is, why would users want to take time out to supply these details? Didn’t they install the app to get parking details rather than supplying them? This is where the app’s incentive-driven approach kicks in. Every user needs a minimum of 5 ‘points’ to be able to use the app.  We initially provide every user with 20 free points. Whenever users submit parking information they gain one point and whenever they successfully find a parking they pay two points. These points work as virtual money while providing the same benefits as real money. Of note is that when users vacate their parking, they must inform the app in order to earn their point and help the app in categorizing this spot as open.

Lastly, it is worth mentioning that our app is capable of informing users if a questionable place is safe for parking or not. For e.g in NYC, a lot of places that usually look like normal parking spots are actually reserved only for some specific vehicle categories. Therefore our app eliminates the need of asking strangers where to park, or adding more cash to NYC's $600 million yearly revenue from parking tickets.

<b>Technologies:</b>
Userend: Android(Java)
Backend: Django(Python)
Datastore: AWS, Elastic Search

<b>Future Scope:</b>
However, this technique invites a new problem of addressing cases where users feed wrong or fake data in order to gain points. To tackle this issue, we employ a complaint recording mechanism which keeps track of fraud cases lodged by our customers. Note that when users feed data to the app, their profile information also gets stored every time. This helps in finding users accused for posting incorrect data several times. At first, we just give them a warning. However, if complaints against them continue, we permanently block such users as we believe that this is the only way of preventing people from adopting wrong practices. 
