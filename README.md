# Doco
Doco (Document Converter) is a lightweight Java library used to convert (from and to) indexed Documents provided by Search API in Google App Engine.

### Homepage
http://www.vidolima.com/projects/doco

# How to Use Doco

## Annotations:

### @DocumentIndex

This annotation is applied to the entity class. This annotation defines the name of the IndexSpec.
Doco by default assume the name of the class as index id
```java
// the name of the index id will be "Foo"
@DocumentIndex
public class Foo {}
```
You can set the index id as you want using the parameter ___name___
```java
@DocumentIndex(name = "customIndexId")
public class Foo {}
```
### @DocumentId

Place this annotation on fields of an entity POJO. This field defines the id of the document.
P.S: the id must be ___String___. In many cases the indentifier of an entity is ___Long___, you can keep this field as ___Long___ and create an ___String___ field and use it as a convertion.

```java
@DocumentId
private String id;
```

If the ___name___ parameter was not defined the 

### @DocumentField

Place this annotation on fields of an entity POJO. This annotation defines a field of a document.
All fields must have a name and a type (see all types here).
Doco assumes the same name of the annotated property to the name of the document field if the name was not specified, and assumes ___TEXT___ type by default.

```java
@DocumentField
private String textField;
```
Use de ___name___ property to specify a name to the field
```java
@DocumentField(name = "otherName")
private String textField;
```
You can specify the field type using the ___type___ parameter
```java
@DocumentField(type = FieldType.NUMBER)
private Double total;
```


# Requirements
* Java 1.5+
* Google App Engine Java SDK 1.8.5+

# TODO
* Default type for: NUMBER, DATE and GEO_POINT
* Convert collections

# See Also
Search API documentation
https://developers.google.com/appengine/docs/java/search/

# License
Doco is released under the terms of the MIT license.
