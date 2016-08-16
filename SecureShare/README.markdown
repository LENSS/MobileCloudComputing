# Scott Huchton's SecureShare code

This is the source code edits log for my thesis titled [*Mobile Distributed File System*](https://github.com/shuchton/Secure-Share) by [Scott Huchton](mailto:sthuchto@nps.edu).

#February 3, 2011
I'm just about done.  I'm sure there are leaks here and there that need to be cleaned up, but for the most part, the networking works as advertised.  So far I've just tested this using the network interface on the same phone, but the next step will be to build a simple directory service that can act as a central repository for the phones since I am not going to tackle the network flooding issue for the thesis.  Geoff and I can work that out later.

Messaging works by sending *SecureShareMessage* messages.  All messages are derived from this base class and any transmission of data between the phones is encapsulated in one of these classes.  The reason is that the Server can identify the type of message and take appropriate action.

It might be wise for future work to formalize the message system a bit more and do it the way proper network packets or frames are done.  Meaning that we can design a header for our packet which includes things like type and length so that the system could be extended to other devices that don't run Java.  That's the 100 yard view for now though.  I'll just be happy if the demo works. ;)

#February 2, 2011
Lots of changes to the ad-hoc networking.  Android still requires an AP to talk to each other.  However, I've implemented a server service and a network client that allows the transfer of messages from one phone to another wirelessly.

#January 25, 2011
Files are stored to internal storage.  So far only pictures work.  I suspect that a transition to larger files will require moving the storage to external storage such as an SD card.  What remains is to get the ad-hoc networking to function.  Once that works, I will go back and improve on storage.

# January 17, 2011
Finished the select picture Activity.  For now, this application will support only pictures.  However, given that the file select activity is modular and MDFS works off the filePath, any number of additional formats could be utilized.

I will now begin to store file fragments locally.  Once the fragments are stored locally, I will attempt to recover the stored files and view the contents of the file on the phone using those fragments.

The next step will be to flesh out the inter-phone communication and transfer fragments for remote storage.

# January 8, 2011
Successfully tested the MDFS native code running on Android.  I don't have any of the networking stuff working yet.

I had to rewrite a bit of the C file because I moved the code to a different package (from the net.shuchton to edu.nps package structure that I started using).  Just a note on how to get the native code to compile in Android...

1. Put all the native code in the \[PROJECT_ROOT\]/jni directory.  Make sure the java code has compiled for the JNI wrapper.  In my case, this is the *edu.nps.secureshare.reedsolomon.ReedSolomon* class.
2. Change to the \[PROJECT_ROOT\]/bin directory and type `javah -jni edu.nps.secureshare.reedsolomon.ReedSolomon` to generate the proper .h file
3. Move the generated .h file to the \[PROJECT_ROOT\]/jni directory and type `ndk-build`

*Note: if you must build again, be sure to type `ndk-build clean`*

Right now, the n and k are hardcoded at 7 and 4 respectively.  However, I am thinking of making that a SharedPref option that can be changed in the program settings.

I need to begin to work on the networking aspect so that I can actually transfer files.  So far, all is going well, but I haven't had to deal with network lag yet.  I hope the UI will remain responsive because most of the heavy lifting is done in the background using services and threads.

# January 7, 2011
Halted work for a bit to write part of the thesis that goes with this code.  An initial Android GUI is present and I've started work on the filer server service that will run on the phone to handle incoming requests.

# December 3, 2010
This is the initial commit for the Android version of this software.  The Shamir code works, but needs to be cleaned up.  The Android Activity does nothing at this point.  The native code has compiled successfully using the Android NDK, so I think all the building blocks are here.

My next step is to create a file chooser.  I will utilize internal storage rather than shared storage for security purposes.  I intend to use GET\_INTENT to choose photos, videos, and locations from the phone.  I will also provide a chooser to list files that are stored on the phone.  I will need to think that out a bit more.

Previous logs appear below...
* * *

# November 28, 2010
Got Shamir's Algorithm working.  The library is a very slightly modified (I took the command line out and just used the engine) version of [Shamir Secret Sharing in Java](http://sourceforge.net/apps/trac/secretsharejava/wiki) by [Tim Tiemens](http://sourceforge.net/users/timtiemens).  It is a Java implementation of Shamir's Secret Sharing algorithm as described in Applied Cryptography \(as LaGrange Interpolating Polynomial Scheme\).

Now all the building blocks to complete my program are in place.  Now I need to port to Android to test.

# November 20, 2010 #
I realized that I was doing VC all wrong and my code was getting hard to read, so I pulled the most current version of my code and conducted an initial commit

Things that need to be completed 
*	Get Sharmir's Algorithm Code working   
*	Create a simple Android application to test my program

I created the branch Shamirs-Algorithm to start work on getting a java version of Shamir's Algorithm working.

Shamir's algorithm uses a string to encode.  Therefore, I will take the rawSecretKey byte[] array and convert it to a Base64 String to pass to Shamir's algorithm.  Then I will find the string and decode it back to the byte[] array to use with the AES encryption scheme.