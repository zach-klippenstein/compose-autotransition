# compose-autotransition

_Status: Experimental_

A simple library for automatically animating between Compose states.

```kotlin
var scale by remember { mutableStateOf(1f) }
var elevation by remember { mutableStateOf(0.dp) }

with(rememberAutoTransition()) {
    Button(onClick = {
        withAnimation {
            scale = 0.5f
            elevation = 8.dp
        }
    }) { /* … */ }
}
```

The main component of this library is the `AutoTransition.withAnimation` function, which takes a
block and animates changes to any snapshot state objects (such as those created by `mutableStateOf`)
written to inside the block. Use the `rememberAutoTransition()` Composable function to get an
instance of `AutoTransition`.

## Demo

https://user-images.githubusercontent.com/101754/178158724-e67477be-4569-48c9-9955-cf5d01f1ccd9.mp4

## Customization

By default, `withTransition` can animate changes to any `MutableState` objects holding values of
types that Compose ships with `TwoWayConverter` vector converters for. This set is defined in
`DefaultAdapters.kt`. To specify how to handle additional types (either additional state object
types or value types inside `MutableState`s), provide a custom `AutoTransitionFactory` using
`AutoTransitionFactory.Builder` and `LocalAutoTransitionFactory`.

Here's an example of providing an adapter for a custom value type:

```kotlin
class Temp(val degrees: Float)

@Composable
fun App() {
    val factory = remember {
        AutoTransitionFactory.Builder()
            .addAdapterRegistry(MutableStateAdapter(
                Temp::class,
                TwoWayConverter(
                    convertToVector = { /* … */ },
                    convertFromVector = { /* … */ }
                )
            ))
            .build()
    }

    CompositionLocalProvider(LocalAutoTransitionFactory provides factory) {
        // Rest of app.
        HomeScreen()
    }
}

@Composable
fun HomeScreen() {
    with(rememberAutoTransition()) {
        // …
    }
}
```

The `AutoTransitionFactory.Builder` can also override the default animation spec.

## TODO

- [x] Write docs.
- [ ] Release on Maven.
- [ ] Support multiplatform.
