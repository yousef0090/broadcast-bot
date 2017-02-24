# Wire™

[![Wire logo](https://github.com/wireapp/wire/blob/master/assets/header-small.png?raw=true)](https://wire.com/jobs/)

# Open source

The [privacy page](https://wire.com/privacy/) and the [privacy](https://wire.com/resource/Wire%20Privacy%20Whitepaper/download/) and [security](https://wire.com/resource/Wire%20Security%20Whitepaper/download/) whitepapers explain the details of the encryption algorithms and protocols used.

For licensing information, see the attached LICENSE file and the list of third-party licenses at [wire.com/legal/licenses/](https://wire.com/legal/licenses/).

If you compile the open source software that we make available from time to time to develop your own mobile, desktop or web application, and cause that application to connect to our servers for any purposes, we refer to that resulting application as an “Open Source App”.  All Open Source Apps are subject to, and may only be used and/or commercialized in accordance with, the Terms of Use applicable to the Wire Application, which can be found at https://wire.com/legal/#terms.  Additionally, if you choose to build an Open Source App, certain restrictions apply, as follows:

a. You agree not to change the way the Open Source App connects and interacts with our servers; b. You agree not to weaken any of the security features of the Open Source App; c. You agree not to use our servers to store data for purposes other than the intended and original functionality of the Open Source App; d. You acknowledge that you are solely responsible for any and all updates to your Open Source App. 

For clarity, if you compile the open source software that we make available from time to time to develop your own mobile, desktop or web application, and do not cause that application to connect to our servers for any purposes, then that application will not be deemed an Open Source App and the foregoing will not apply to that application.

No license is granted to the Wire trademark and its associated logos, all of which will continue to be owned exclusively by Wire Swiss GmbH. Any use of the Wire trademark and/or its associated logos is expressly prohibited without the express prior written consent of Wire Swiss GmbH.

# Broadcast bot
This Bot can be used as broadcast channel to reach a broad audience. In order to subscribe to a channel user should use the link: https://wire.com/b/[channel_name]. There is no limitation in the number of subscribers to a channel or number of messages sent to them.
 
Once the message is broadcast it will take couple of seconds to be delivered to all subscribers. Message can be: text, URL, picture (a url to jpg|png|gif), YouTube link, SoundCloud link, etc. There is currently no support to send audio or video files natively.

All messages are stored in broadcast.db SQLite DB on the server. Incoming messages from the subscribers are also stored in broadcast.db. 
  
## Build the project
 Run: `$make linux` 
 
*windows* and *darwin* are also supported. Running *make* for the first time will generate self signed certificate (stored in ./certs folder).
 Modify the Makefile before the run in order to better reflect your company's name/country...

## Register as Wire Bot Developer 
This is done using DevBot. Go to https://wire.com/b/devbot and log in with your Wire credentials 
- "DevBot" is a bot to set up your developer account and create your own bots. 
More info on how to register and setup bots can be found here: https://github.com/wireapp/wire-bot-java

## Create new bot
Again through DevBot: Create your bot (in this case it will be a bot to serve as a channel). 
Pick up some unique/catchy name for the bot.

## Deployment
Deploy:
```
broadcast.jar
broadcast.yaml
keystore.jks
```
files to your server. Notice that you will need a Public IP to serve as endpoint that will be called by the Wire Backend.

## Run the bot
Run: `$java -jar broadcast.jar server broadcast.yaml`

## Broadcast
Go to: http://localhost:8049/assets/message.html and post your first message.

## Setup Feedback conversation
In order to receive all the incoming messages from the subscribers you can select an existing conversation with the broadcast bot and let the service channel all the messages there. Update `feedback` param in `broadcast.yaml` with the BotId. BotId can be extracted from the logs.
