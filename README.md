# Androiddatetimetextprovider

A solution for day and month formatting for languages with standalone configuration for ThreeTenBP for Android

## tl;dr

Do you need to format dates to locales with specific standalone configurations for Month or DayOfWeek like catalan, polish, finnish, or others? This is the solution.

## What is this?

I've been using [ThreeTenBp](https://github.com/ThreeTen/threetenbp) for Android projects for a long time. It's a great project that allows me to use Java 8 date/time framework without any problem.

But, recently I found an issue in an application I had to translate into catalan. Basically:

- In catalan, the preposition used in front of month for dates changes depending on the month (8 *de* febrer vs. 8 *d'* abril)
- In order to fix this, the `MMMM` pattern includes the preposition (so, `d MMMM` will generate `8 de febrer` or `8 d'abril` as the `de` and `d'` are part of the month name.
- What happens when you need to generate just the name of the month without preposition? That's when `LLLL` comes to the rescue.
- Theorically, Java supports `LLLL` in date formatting. But not exactly. For languages like catalan `LLLL` and `MMMM` generate exactly the same output.
- The worst part, is that Android does it right, and ye olde `SimpleDateFormat` returns diferent month names for `LLLL` and `MMMM`. Also, `LLLL` vs `MMMM` is well documented in the `SimpleDateFormat` javadoc (even using catalan as example) but no mention about it in `DateTimeFormatter`

Then I started to investigate, I've spent hours looking at repos, documentation and standards, but basically:

- As android is using the right standards, the information is there. But, as ThreeTenBP mimics Java behaviour, Java does not expect this info to be there.
- I've claimed about it in this issue (https://github.com/ThreeTen/threetenbp/issues/55) where other users were having the same problems in other languages.
- @sschaap linked to his own answer in other issue with a coherent explanation of what's going on (thanks a lot!)
- Then I saw a link to this issue in another library: https://github.com/gabrielittner/lazythreetenbp/pull/11
- Here @pamalyshev offers a solution in a PR for Android. But the answer of the library author is quite right, do not force it, as is not following the idea of ThreeTenBp (mimic Java 8), but offer as a separate library/solution.
- And then the discussion is dead. @pamalyshev cloned the repo with the solution, but it's never published as a library (or I didn't see it).
- Then I decided to publish this solution as a standalone solution.

Keep in mind I'm simplifying the investigation process, as it's quite long and I'm sure not many of you look at huge xml of language configurations with the same excitement than me.

Basically, this solution is not created by me, it's the collected effort of different individuals and put it together for your convenience. 

## How to use it?

You need to include three libraries in your app.

- ThreeTenBP (https://github.com/ThreeTen/threetenbp). The original backport of the JSR-310.
- Lazythreetenbp (https://github.com/gabrielittner/lazythreetenbp). A lazy loading ZoneRuleProvider for ThreeTenBp. This is used in Android as the zones information should be loaded before using ThreeTenBp and it could be a noticeable long time for an Android app startup. This library implements a lazy loading mechanism for that information. 
- This cute small library to fix the issue.

So, add to your gradle:

1. The jitpack.io as repository. This is a small library and it's the fastest way of publishing it.

```
allprojects {
  repositories {
	...
	  maven { url 'https://jitpack.io' }
	}
}
```

2. All the dependencies needed

``` 
implementation "com.gabrielittner.threetenbp:lazythreetenbp:0.7.0"
implementation "org.threeten:threetenbp:1.4.0:no-tzdb"
implementation 'com.github.sergiandreplace:androiddatetimetextprovider:1.0'
```

3. And now you can use both LazyThreeTenBp and AndroidDateTimeTextProvider in your init part (tipically in you Application class):
```
LazyThreeTen.init(context);
DateTimeTextProvider.setInitializer(AndroidDateTimeTextProvider());
```

And that's it!

## Testing

If you want to test that it works, there is a couple of test files that will tests the formatting for Months and Days of week for Catalan, Finnish, Polish and Russian. 

If there is another language that could benefit from this solution and I'm not aware, feel free to open an isse to check it, or if you feel extra proactive, open a PR adding it to the tests.


## The real heroes

As I said, I've just joined other people work, so this people is the one to thank for tihs solution:

- @jodastephen and the rest of people who worked in the ThreeTenBP project. A real life-saver!
- @gabrielittner for creating LazyThreeTenBp
- @sschaap for his great explanations and patience answering
- @pamalyshev for creating the solution and doing the changes needed to ThreeTenBp to apply it.

## Disclaimer

All the information I wrote here is my understanding of the problem the solution and how the involved people has participated. If there is something wrong or misleading do not hesitate to say so and I will apply the right corrections.

Have fun!
