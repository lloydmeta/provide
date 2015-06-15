# @Provide Scala annotation [![Build Status](https://travis-ci.org/lloydmeta/provide.svg?branch=master)](https://travis-ci.org/lloydmeta/provide) 
 
Have you ever wanted to make sure that your code implements a method in a parent class/trait but thought that 
[`override` as too dangerous/wrong](http://stackoverflow.com/questions/5643144/using-the-override-keyword-on-implementations-of-abstract-methods)?

This will help you gain code clarity without sacrificing runtime soundness. 

This is still a WIP. Issues, PRs welcome.

## SBT

At the moment, only compatible with 2.11.x

For 
```scala
libraryDependencies ++= Seq(
    "com.beachape" %% "provide" % "0.0.1-SNAPSHOT"
)
```

This library makes use of Macro annotations, so you will also need to [enable macro paradise](http://docs.scala-lang.org/overviews/macros/paradise.html)
in your build.

## Usage

```scala
import com.beachape.annotations._

// The following will fail at compile time, telling you that there is no such method in parent classes
trait A {
  def i(x: Int): Int
}

class Test extends A {
  def i(y: Int) =  4
  @provide def i = 3
}

// The following will fail because you aren't providing any implementation for the method you claim to be providing
trait A {
  def i: Int
}

trait B extends A {
  @provide def i: Int
}

/* 
  The following silliness will also fail, because you are saying you are providing an implementation, but you
  are actually overriding an existing method. This could result in serious runtime errors, so this lib helps
  you fix that problem.
 */
trait A {
  def i = 3
}

trait B extends A {
  @provide override def i = 9
}

// The following will work fine
trait A {
  def i(x: Int): Int
}

class Test extends A {
  @provide  def i(y: Int) =  4
  def i = 3
}

```

## Licence

The MIT License (MIT)

Copyright (c) 2015 by Lloyd Chan

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
