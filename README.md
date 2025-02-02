# VOCI

This project is the result of the software-development course at the OTH-Regensburg. 

## Project "Voci - Chatsystem"

Voci is a chat-system for sending messages in calls or chatrooms.
Unfortunately voice-streaming is not supported, as first planned. 

#### REST-API

I imported swagger-ui in my project to visualize my REST-API. It is available at: http://<host>:8945/swagger-ui.

To view swagger-ui, you need to be logged in with an account.


### "Tutorial"

To communicate with others create a room with text-channels or start a new call, to which you can invite your contacts or guests.

Inviting contacts should be intuitive. To invite guests, copy the access-token, after you started a call and send it to the guests per email or other messengers. 

The guest can join at http://<host>:8945/invitation, where he can enter the access-token.

Now you can chat. 

If you want to send files from your Dropsi-account. Log in to your Dropsi-account and copy the secret-key to your clipboard.
Under INFO - (http://<host>:8945/info), you can enter this token, to connect Dropsi and VOCI.

Now you can see an upload-button in your calls or rooms next to the send button, with which you can upload your Dropsi-files to the text-channel.

Every user in the channel can download the file. 
