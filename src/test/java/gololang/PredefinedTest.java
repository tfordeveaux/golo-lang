/*
 * Copyright 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gololang;

import org.testng.annotations.Test;

import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.WrongMethodTypeException;
import java.util.concurrent.Callable;

import static java.lang.invoke.MethodType.genericMethodType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import fr.insalyon.citi.golo.runtime.AmbiguousFunctionReferenceException;

public class PredefinedTest {

  @Test
  public void require_1_is_1() {
    Predefined.require(1 == 1, "1 should be 1");
  }

  @Test(expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = "1 should be 2")
  public void require_1_is_2() {
    Predefined.require(1 == 2, "1 should be 2");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void require_non_boolean_condition() {
    Predefined.require("foo", "bar");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_require_non_string_message() {
    Predefined.require(1 == 1, 666);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void test_array_manipulation() {
    // these Predefined functions are deprecated. We still test them though.
    Object[] data = (Object[]) Predefined.Array(1, 2, 3, "foo", "bar");
    assertThat((Integer) Predefined.alength(data), is(5));

    assertThat((Integer) Predefined.aget(data, 0), is(1));
    assertThat((String) Predefined.aget(data, 3), is("foo"));

    Predefined.aset(data, 0, "plop");
    assertThat((String) Predefined.aget(data, 0), is("plop"));
  }

  @Test
  public void test_require_not_null_ok() {
    Predefined.requireNotNull("foo");
  }

  @Test(expectedExceptions = AssertionError.class)
  public void test_require_not_null_fail() {
    Predefined.requireNotNull(null);
  }

  @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "ok")
  public void test_raise() {
    Predefined.raise("ok");
  }

  @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "ok")
  public void test_raise_with_cause() {
    try {
      Predefined.raise("ok", new IOException());
    } catch (RuntimeException expected) {
      assertThat(expected.getCause(), notNullValue());
      assertThat(expected.getCause(), instanceOf(IOException.class));
      throw expected;
    }
  }

  @Test
  public void test_range() {
    assertThat(Predefined.range(1, 10), instanceOf(IntRange.class));
    assertThat(Predefined.range(1, 10L), instanceOf(LongRange.class));
    assertThat(Predefined.range(1L, 10), instanceOf(LongRange.class));
    assertThat(Predefined.range(1L, 10L), instanceOf(LongRange.class));
    assertThat(Predefined.range(10), instanceOf(IntRange.class));
    assertThat(Predefined.range(10L), instanceOf(LongRange.class));
    assertThat(Predefined.range(10), is(Predefined.range(0, 10)));
    assertThat(Predefined.range(10L), is(Predefined.range(0L, 10L)));
    assertThat(Predefined.range('a', 'd'), instanceOf(CharRange.class));
    assertThat(Predefined.range('D'), instanceOf(CharRange.class));
    assertThat(Predefined.range('D'), is(Predefined.range('A', 'D')));
  }

  @Test
  public void test_reversed_range() {
    assertThat(Predefined.reversed_range(10, 1), instanceOf(IntRange.class));
    assertThat(Predefined.reversed_range(10, 1L), instanceOf(LongRange.class));
    assertThat(Predefined.reversed_range(10L, 1), instanceOf(LongRange.class));
    assertThat(Predefined.reversed_range(10L, 1L), instanceOf(LongRange.class));
    assertThat(Predefined.reversed_range(10), instanceOf(IntRange.class));
    assertThat(Predefined.reversed_range(10L), instanceOf(LongRange.class));
    assertThat(Predefined.reversed_range(10), is(Predefined.reversed_range(10, 0)));
    assertThat(Predefined.reversed_range(10L), is(Predefined.reversed_range(10L, 0L)));
    assertThat((IntRange)Predefined.reversed_range(5, 1), is(((IntRange)Predefined.range(5, 1)).incrementBy(-1)));
    assertThat((LongRange)Predefined.reversed_range(5L, 1L), is(((LongRange)Predefined.range(5L, 1L)).incrementBy(-1)));
    assertThat(Predefined.reversed_range('d', 'a'), instanceOf(CharRange.class));
    assertThat(Predefined.reversed_range('D'), instanceOf(CharRange.class));
    assertThat(Predefined.reversed_range('D'), is(Predefined.reversed_range('D', 'A')));
    assertThat((CharRange)Predefined.reversed_range('D', 'A'), is(((CharRange)Predefined.range('D', 'A')).incrementBy(-1)));
  }

  static class MyCallable {

    static Object hello() {
      return "Hello!";
    }

    static Object overloaded(int a, int b) {
      return a + b;
    }

    static Object overloaded(int a) {
      return a + 1;
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void test_asInterfaceInstance() throws Throwable {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodHandle handle = lookup.findStatic(MyCallable.class, "hello", genericMethodType(0));
    assertThat((String) handle.invoke(), is("Hello!"));
    Callable<Object> converted = (Callable<Object>) Predefined.asInterfaceInstance(Callable.class, handle);
    assertThat((String) converted.call(), is("Hello!"));
  }

  @Test(expectedExceptions = WrongMethodTypeException.class)
  public void test_asInterfaceInstance_wrong_target_type() throws Throwable {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodHandle handle = lookup.findStatic(MyCallable.class, "hello", genericMethodType(0));
    assertThat((String) handle.invoke(), is("Hello!"));
    Predefined.asInterfaceInstance(ActionListener.class, handle);
  }

  @Test
  public void test_fun() throws Throwable {
    MethodHandle hello = (MethodHandle) Predefined.fun("hello", MyCallable.class, 0);
    assertThat((String) hello.invoke(), is("Hello!"));
  }

  @Test
  public void test_fun_no_arity() throws Throwable {
    MethodHandle hello = (MethodHandle) Predefined.fun("hello", MyCallable.class);
    assertThat((String) hello.invoke(), is("Hello!"));
  }

  @Test(expectedExceptions = NoSuchMethodException.class)
  public void test_fun_fail() throws Throwable {
    MethodHandle hello = (MethodHandle) Predefined.fun("helloz", MyCallable.class, 0);
  }

  @Test(expectedExceptions = AmbiguousFunctionReferenceException.class)
  public void test_fun_ambiguous() throws Throwable {
    MethodHandle overloaded = (MethodHandle) Predefined.fun("overloaded", MyCallable.class);
  }

  @Test(expectedExceptions = WrongMethodTypeException.class)
  public void test_fun_wrong_arity() throws Throwable {
    MethodHandle overloaded = (MethodHandle) Predefined.fun("overloaded", MyCallable.class, 1);
    overloaded.invoke(1, 2);
  }

  @Test
  public void test_fun_overloaded1() throws Throwable {
    MethodHandle overloaded = (MethodHandle) Predefined.fun("overloaded", MyCallable.class, 1);
    assertThat((Integer) overloaded.invoke(2), is(3));
  }

  @Test
  public void test_fun_overloaded2() throws Throwable {
    MethodHandle overloaded = (MethodHandle) Predefined.fun("overloaded", MyCallable.class, 2);
    assertThat((Integer) overloaded.invoke(1, 2), is(3));
  }

  @Test
  public void test_fileToText() throws Throwable {
    Object content = Predefined.fileToText("THIRD-PARTY", "UTF-8");
    assertThat(content, instanceOf(String.class));
    String text = (String) content;
    assertThat(text, containsString("ASM"));
    assertThat(text, containsString("INRIA"));
    assertThat(text, containsString("DAMAGE"));
    assertThat(text, containsString("INSA-Lyon"));
  }

  @Test
  public void test_textToFile() throws Throwable {
    File tempFile = File.createTempFile("plop", "daplop");
    String message = "Plop!";
    Predefined.textToFile(message, tempFile);
    String text = (String) Predefined.fileToText(tempFile, "UTF-8");
    assertThat(text, is(message));
  }

  @Test
  public void test_fileExists() throws Throwable {
    File tempFile = File.createTempFile("this_exists", "test");
    assertThat(Predefined.fileExists(tempFile), is(true));
  }

  @Test
  public void test_isArray() {
    assertThat(Predefined.isArray(null), is(false));
    assertThat(Predefined.isArray(Object.class), is(false));
    assertThat(Predefined.isArray(new Object[]{}), is(true));
  }

  @Test
  public void test_arrayOfType() throws ClassNotFoundException {
    assertThat((Class) Predefined.arrayTypeOf(Object.class), sameInstance((Class) Object[].class));
    assertThat((Class) Predefined.objectArrayType(), sameInstance((Class) Object[].class));
  }

  @Test
  public void check_value_conversions() {
    assertThat(Predefined.intValue(1), is((Object) 1));
    assertThat(Predefined.intValue(1L), is((Object) 1));
    assertThat(Predefined.intValue(1.0d), is((Object) 1));
    assertThat(Predefined.intValue("1"), is((Object) 1));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void check_bogus_value_conversion() {
    Predefined.doubleValue(new Object());
  }
}
