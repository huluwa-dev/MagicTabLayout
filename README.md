# MagicTabLayout
This is a magic TabLayout.

## How to use it
Add this line to build.gradle.
```
implementation 'com.huluwa.lib.magictablayout:MagicTabLayout:1.0.1'
```

And then use it like the code below.
```
<com.huluwa.lib.magictablayout.MagicTabLayout
        android:id="@+id/magicLayout"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:paddingHorizontal="5dp"
        app:animateSelected="true"
        app:bgColor="@color/colorAccent"
        app:bottomRadius="10dp"
        app:gapSize="4dp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:normalTextColor="@color/white"
        app:normalTextSize="13sp"
        app:selectDrawable="@drawable/huluwa"
        app:selectTextColor="@color/red"
        app:selectedTextSize="14sp"
        app:titleIconDrawable="@drawable/icon"
        app:titleIconPadding="4dp"
        app:topRadius="10dp" />
```

![image](https://github.com/huluwa-dev/MagicTabLayout/blob/master/preview.gif)
