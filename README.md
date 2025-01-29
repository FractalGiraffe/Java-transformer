# Java-transformer
Java source code transformer, utlising BCEL, for high level code optimisation.

Transformer that implements peephole optimisation as much as possible. Javac already does some of this. E.g. the following Java code:

- Constant folding example
```
int a = 429879283 - 876987;
// no assignment to a in the middle
System.out.println(527309 - 1293 + 5 * a);
```

produces the following constant pool in the corresponding .class file:

- Bytecode constant pool
```
const #2 = int 429002296 // this would be variable a
...
const #7 = int 526016 // this would be the subexpression in println
``` 

We can see that javac has performed 2 arithmetic operations to achieve constant folding, 429879283 - 876987 and 527309 - 1293. However, if there's no assignment to `a`, it can be folded further by propagating the value of a. We identify such patterns here and perform folding as much as possible.

Dir structure:
```
src -- main
             -- Main.java
             -- ConstantFolder.java
   -- target
             -- SimpleFolding.j
             -- ConstantVariableFolding.java
             -- DynamicVariableFolding.java
test
lib
build.xml
```
