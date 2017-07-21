package com.vidolima.doco;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.GeoPoint;

//import org.apache.log4j.Logger;

@RunWith(JUnit4.class)
public class DocoTest {
	
//	private final transient Logger logger = Logger.getLogger(DocoTest.class);
	
    @Test
    public void testConversionFromDocumentToObject() {

        Foo t = new Foo();
        t.setCode(1);
        t.setAtomFieldTest("This is a Atom field");
        t.setDateFieldTest(new Date());
        t.setGeopointFieldTest(new GeoPoint(1, 0));
        t.setHtmlFieldTest("<html><title>Doco</title><body>Test doco</body></html>");
        t.setNumberFieldTest(1986d);
        t.setTextFieldTest("Text field declararion with name and type");
        t.setTextFieldWithoutName("Text field declaration without name");
        t.setTextFieldWithoutType("Text field declaration without type");
        t.setTextFieldWithoutTypeAndName("Simple text field deplaration without type and name");

        Doco doco = new Doco();
        Document doc = doco.toDocument(t);

        Assert.assertEquals(Integer.valueOf(doc.getId()), t.getCode());
        Assert.assertEquals(doc.getOnlyField("atomFieldTest").getAtom(), t.getAtomFieldTest());
        Assert.assertEquals(doc.getOnlyField("dateFieldTest").getDate(), t.getDateFieldTest());
        Assert.assertEquals(doc.getOnlyField("geopointFieldTest").getGeoPoint(), t.getGeopointFieldTest());
        Assert.assertEquals(doc.getOnlyField("htmlFieldTest").getHTML(), t.getHtmlFieldTest());
        Assert.assertEquals(doc.getOnlyField("numberFieldTest").getNumber(), t.getNumberFieldTest());
        Assert.assertEquals(doc.getOnlyField("justText").getText(), t.getTextFieldTest());
        Assert.assertEquals(doc.getOnlyField("textFieldWithoutName").getText(), t.getTextFieldWithoutName());
        Assert.assertEquals(doc.getOnlyField("txtName").getText(), t.getTextFieldWithoutType());
        Assert.assertEquals(doc.getOnlyField("textFieldWithoutTypeAndName").getText(),
            t.getTextFieldWithoutTypeAndName());
    }

    @Test
    public void testConversionFromObjectToDocument() {

        Document doc = Document
            .newBuilder()
            .setId("123456")
            .addField(Field.newBuilder().setName("atomFieldTest").setAtom("This is a Atom field"))
            .addField(Field.newBuilder().setName("dateFieldTest").setDate(new Date()))
            .addField(Field.newBuilder().setName("geopointFieldTest").setGeoPoint(new GeoPoint(1, 0)))
            .addField(
                Field.newBuilder().setName("htmlFieldTest")
                    .setHTML("<html><title>Doco</title><body>Test doco</body></html>"))
            .addField(Field.newBuilder().setName("numberFieldTest").setNumber(1986))
            .addField(Field.newBuilder().setName("justText").setText("Text field declararion with name and type"))
            .addField(Field.newBuilder().setName("textFieldWithoutName").setText("Text field declaration without name"))
            .addField(Field.newBuilder().setName("txtName").setText("Text field declaration without type"))
            .addField(
                Field.newBuilder().setName("textFieldWithoutTypeAndName")
                    .setText("Simple text field deplaration without type and name")).build();

        Doco doco = new Doco();
        Foo f = doco.fromDocument(doc, Foo.class);

        Assert.assertEquals(Integer.valueOf(doc.getId()), f.getCode());
        Assert.assertEquals(doc.getOnlyField("atomFieldTest").getAtom(), f.getAtomFieldTest());
        Assert.assertEquals(doc.getOnlyField("dateFieldTest").getDate(), f.getDateFieldTest());
        Assert.assertEquals(doc.getOnlyField("geopointFieldTest").getGeoPoint(), f.getGeopointFieldTest());
        Assert.assertEquals(doc.getOnlyField("htmlFieldTest").getHTML(), f.getHtmlFieldTest());
        Assert.assertEquals(doc.getOnlyField("numberFieldTest").getNumber(), f.getNumberFieldTest());
        Assert.assertEquals(doc.getOnlyField("justText").getText(), f.getTextFieldTest());
        Assert.assertEquals(doc.getOnlyField("textFieldWithoutName").getText(), f.getTextFieldWithoutName());
        Assert.assertEquals(doc.getOnlyField("txtName").getText(), f.getTextFieldWithoutType());
        Assert.assertEquals(doc.getOnlyField("textFieldWithoutTypeAndName").getText(),
            f.getTextFieldWithoutTypeAndName());
    }

    /**
     * Tests whether subclass can be converted to a document successfully or not.
     * 
     * Timeout is necessary so that infinite loop can be detected.
     */
    @Test(timeout = 2000)
    public void testCreateDocumentForSubclass() {
        Bar bar = new Bar();
        bar.setCode(10); // set id
        bar.setSubClassNumberField(50L); // set a field of subclass

        Doco doco = new Doco();
        Document document = doco.toDocument(bar);
        Assert.assertNotNull(document);

        Bar retrievedBar = doco.fromDocument(document, Bar.class);
        Assert.assertEquals(bar.getCode(), retrievedBar.getCode());
        Assert.assertEquals(bar.getSubClassNumberField(), retrievedBar.getSubClassNumberField());
    }
    
    /**
     * James Huang Test if the  DocumentCollection works when converting a document back into a java object which uses the DocumentCollection Annotation
	 * Note: Many fields with the same name in a Document represents a multivalued property and we want to test if we can recognize a multivalued property
	 * and turn it into the appropriate Collection for the Java Object 
     */
    @Test
    public void testDocummentCollectionAnnotationFromDoc(){
    	/**
    	 * Step 1: Create a document with multi-valued property
    	 */
    	Document.Builder doc = Document.newBuilder();
    	doc.setId("123123123");
    	//add a collection of fields which is basically many fields with the same name but different values 
    	for( int i = 0; i < 100; i++){
    		doc.addField( Field.newBuilder().setName( Foo.ARRAY_LIST_TEST ).setAtom("arrItem" + i));
    	}
    	
    	Document docWithMVP = doc.build();
    	//please work
    	
    	Doco doco = new Doco();
    	Foo f = doco.fromDocument(docWithMVP, Foo.class);
    	
//    	TODO: use logger instead of println
    	System.out.println( f.getArrayListTest() );
    	Assert.assertNotNull( f.getArrayListTest() );
    }
    
    @Test
    public void testDocumentCollectionToAndFromDoc(){
    	/**
    	 * Step 1: Create a Foo  with a list
    	 */
    	Foo foo = new Foo(); 
    	foo.setCode(111); // set the id
    	for( int i = 0; i< 100; i++){
    		foo.addToArrayListTest("fooList"+ i); // set the list property
    	}
    	
    	//come on please deploy properly this time
    	
    	/**
    	 * Step 2: Convert it to a Document
    	 */
    	Doco doco = new Doco();
    	Document document = doco.toDocument(foo);
    	
    	/**
    	 * Step 3: Convert it back into Foo
    	 */
    	Foo foo2 = doco.fromDocument(document, Foo.class);
    	Assert.assertNotNull( foo2 );
    	Assert.assertNotNull( foo2.getArrayListTest() );
    	
    }

}