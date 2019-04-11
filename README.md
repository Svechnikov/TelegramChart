# TelegramChart #

Single-activity app. The JSON data is read from the raw directory.
JSON parsing is done with native Android libs.
Night and day themes are implemented with android-themes.
Charts are implemented as custom views with canvas drawing with HW acceleration.
Axis animations are implemented using ValueAnimators and redrawing.
The app supports configuration changes (for example, orientation change) -
all states are saved and restored on Activity recreation.

Scale animations for horizontal axis are not complete.
I implemented them using a power of 2 step
(that is, every possible step must be a power of 2, like in the example animation).
As it turned out, it doesn't work in real life
(for it to work, points always have to be a power of 2 in size, which is not possible in real life).
What would work - drawing points with arbitrary step,
showing and hiding (when needed) every second point when scale changes.
Unfortunately, I didn't have enough time to complete this implementation,
so I didn't include it in the project.

There are two classes for rendering horizontal axis. By default it's AnimatedHorizontalAxisView.
But you can replace it with NotAnimatedHorizontalAxisView (no scale animations) -
in the class MainChartView replace AnimatedHorizontalAxisView with NotAnimatedHorizontalAxisView.

I'm sorry for not very coherent text, I'm writing this text at deep night :)

