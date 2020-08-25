package org.hamcrest.core;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class IsSame<T> extends BaseMatcher<T> {
    private final T object;

    public IsSame(T object2) {
        this.object = object2;
    }

    @Override // org.hamcrest.Matcher
    public boolean matches(Object arg) {
        return arg == this.object;
    }

    @Override // org.hamcrest.SelfDescribing
    public void describeTo(Description description) {
        description.appendText("sameInstance(").appendValue(this.object).appendText(")");
    }

    public static <T> Matcher<T> sameInstance(T target) {
        return new IsSame(target);
    }

    public static <T> Matcher<T> theInstance(T target) {
        return new IsSame(target);
    }
}
