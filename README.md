# Slyce Messaging API

This library is an extension of the original Slyce Messaging library. The changes that have been made are:

 * Added Vector drawable support
 * Removed custom View classes (FontEditText and FontTextView - use Calligraphy instead in the parent project)
 * Added support for RTL in layouts
 * Removed deprecated layout attributes
 * Added attributes to make more elements customisable.

## Installation

Add the following to your app's gradle file:

```ruby
repositories {
    jcenter()
    maven { url "https://s3.amazonaws.com/repo.commonsware.com" }
    maven { url "https://jitpack.io" }
}

dependencies {
    compile 'com.github.sambains:SlyceMessaging:2.0.0'
}
```

## The API

You must initialize the fragment by declaring an XML tag like the following:

```xml
<fragment
            android:name="it.slyce.messaging.SlyceMessagingFragment"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:id="@+id/messaging_fragment"/>
```
