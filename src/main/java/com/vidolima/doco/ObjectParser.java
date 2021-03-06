package com.vidolima.doco;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;

import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Index;
import com.vidolima.doco.annotation.*;
import com.vidolima.doco.exception.AnnotationNotFoundException;
import com.vidolima.doco.exception.IllegalAnnotationDeclarationException;
import com.vidolima.doco.exception.ObjectParseException;

/**
 * This class is used to create objects instances from {@link Document}s.
 * 
 * @author Marcos Alexandre Vidolin de Lima
 * @since January 26, 2014
 */
final class ObjectParser {

    /**
     * Obtaions the name of the Index.
     * 
     * @param clazz
     *            the class
     * @return the name of the {@link Index}
     */
    static String getIndexName(Class<?> clazz) {
        DocumentIndex annotation = clazz.getAnnotation(DocumentIndex.class);

        if (annotation == null) {
            DocumentIndexSubClass indexSubClass = clazz.getAnnotation(DocumentIndexSubClass.class);

            if (indexSubClass == null) {
                throw new AnnotationNotFoundException("There is no @DocumentIndex or @DocumentIndexSubClass annotation declared in " + clazz
                        + " class.");
            }

            String name = indexSubClass.name();

            return (name != null && name.length() > 0)? name : clazz.getSimpleName();
        }
        else {
            String name = annotation.name();
            if (name != null && name.length() > 0) {
                return name;
            }

            return clazz.getSimpleName();
        }
    }

    /**
     * Obtains the name value of the {@link DocumentField} annotation of a given {@link Field} or the name of the
     * {@link Field} by default.
     * 
     * @param field
     * @param annotation
     * @return the specified parameter name of the {@link DocumentField} annotation, or if not specified returns the
     *         name of the field by default
     */
    static String getFieldNameValue(java.lang.reflect.Field field, DocumentField annotation) {
        String name = annotation.name();
        if (name == null || name.trim().length() == 0) {
            name = field.getName();
        }
        return name;
    }
    
    /**
     * Obtains the name value of the {@link DocumentCollection} annotation of a given {@link Field} or the name of the
     * {@link Field} by default.
     * 
     * @author James Huang
     * @param field
     * @param annotation
     * @return the specified parameter name of the {@link DocumentField} annotation, or if not specified returns the
     *         name of the field by default
     */
    static String getFieldNameValue(java.lang.reflect.Field field, DocumentCollection annotation) {
        String name = annotation.name();
        if (name == null || name.trim().length() == 0) {
            name = field.getName();
        }
        return name;
    }
    
    

    /**
     * Obtains the {@link FieldType} of the {@link DocumentField} annotation of a given {@link Field}.
     * 
     * @param field
     * @return
     */
    static FieldType getFieldType(java.lang.reflect.Field field) {
        DocumentField annotation = getDocumentFieldAnnotation(field);
        FieldType type = annotation.type();
        if (type == null) {
            return FieldType.TEXT;
        }
        return type;
    }

    /**
     * Obtains the {@link DocumentField} annotation of a given {@link Field}.
     * 
     * @param field
     *            to get the {@link DocumentField} annotation
     * @return the {@link DocumentField} annotation
     */
    static DocumentField getDocumentFieldAnnotation(java.lang.reflect.Field field) {
        return field.getAnnotation(DocumentField.class);
    }
    
    /**
     * Obtains the {@link DocumentCollection} annotation of a given {@link Field}
     * @param field
     *            to get the {@link DocumentCollection} annotation
     * @return the {@link DocumentCollection} annotation
     */
    static DocumentCollection getDocumentCollectionAnnotation( java.lang.reflect.Field field){
    	return field.getAnnotation(DocumentCollection.class);
    }

    /**
     * Obtains the number value of a DocumentField typed as NUMBER.
     * 
     * @param document
     *            the {@link Document} that contains the field
     * @param field
     *            the field to get the value
     * @param fieldName
     *            the name of the field in Document
     * @return the Object number value
     */
    private Object getDocumentFieldNumberValue(Document document, java.lang.reflect.Field field, String fieldName) {

        Class<?> fieldType = field.getType();

        if (Integer.TYPE.equals(fieldType) || Integer.class.equals(field.getType()))
            return Integer.valueOf(document.getOnlyField(fieldName).getNumber().intValue());
        if (Long.TYPE.equals(fieldType) || Long.class.equals(field.getType()))
            return Long.valueOf(document.getOnlyField(fieldName).getNumber().longValue());
        if (Float.TYPE.equals(fieldType) || Float.class.equals(field.getType()))
            return Float.valueOf(document.getOnlyField(fieldName).getNumber().floatValue());
        if (Double.TYPE.equals(fieldType) || Double.class.equals(field.getType()))
            return document.getOnlyField(fieldName).getNumber();

        throw new ObjectParseException("A DocumentField typed as NUMBER must be Long, Integer, Float or Double.");
    }

    /**
     * Obtains the value of the document field given a {@link Document} and a field name.
     * 
     * @param document
     *            the {@link Document} that contains the field
     * @param field
     *            the field to get the value
     * @return the value of the field
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private Object getDocumentFieldValue(Document document, java.lang.reflect.Field field) throws InstantiationException, IllegalAccessException {
    	
    	//the annotation must be a DocumentField or a DocumentCollection
        DocumentField annotation = getDocumentFieldAnnotation(field); 
        DocumentCollection collectionAnnotation = getDocumentCollectionAnnotation( field );
        
        if(annotation == null && collectionAnnotation == null ){
            throw new IllegalAnnotationDeclarationException("Invalid com.google.appengine.api.search.Field.FieldType: ");
        }
        	
        String fieldName = null;
        if( annotation != null){
        	fieldName = getFieldNameValue(field, annotation);  // gets the fieldName from the annotation incase the Search.Field fieldName is not the default java.lang.reflec.Field name
        }
        else{
        	fieldName = getFieldNameValue(field, collectionAnnotation);
        }
        


        if (document.getFieldCount(fieldName) == 0)
            return null;
        else if( document.getFieldCount(fieldName) == 1 && annotation !=null && collectionAnnotation == null){ //only 1 field and uses @DocumentField and doesn't use @DocumentCollection
	        com.google.appengine.api.search.Field f = document.getOnlyField(fieldName); // if we want to support multi-valued field 
	
	        switch (f.getType()) {
	        case TEXT:
	            return f.getText();
	        case ATOM:
	            return f.getAtom();
	        case HTML:
	            return f.getHTML();
	        case DATE:
	            return f.getDate();
	        case NUMBER:
	            return getDocumentFieldNumberValue(document, field, fieldName);
	        case GEO_POINT:
	            return f.getGeoPoint();
	        }
        }
        else if( document.getFieldCount(fieldName) >= 1 && annotation == null && collectionAnnotation != null){ //if it is a multi-valued field and uses @DocumentCollection and not @DocumentField
        	Object collection = null;
        	
        	if( Collection.class.isAssignableFrom( field.getType() ) ){ //Check if java.lang.reflect.field is a Set or of List
        		collection = getConcreteCollection( collectionAnnotation.type() );
    		}
        	else{
        		throw new IllegalArgumentException("multi-valued field must be of type Collection" ); 
        	}
        	
        	//for every com.google.appengine.api.search.Field
        	for( com.google.appengine.api.search.Field f: document.getFields(fieldName) ){
        		
        		switch (f.getType()) {
    	        case TEXT:
    	            ( (Collection) collection).add( f.getText());
    	            break;
    	        case ATOM:
    	        	( (Collection) collection).add( f.getAtom());
    	            break;
    	        case HTML:
    	        	( (Collection) collection).add( f.getHTML());
    	            break;
    	        default:
    	        	throw new IllegalArgumentException("multi-valued field can only be TEXT, ATOM, or HTML, actual type of search field f:" + f.getType() );
    	        }
        		
        	}
        	
        	return collection;
        }
        //else return null
		return null;

    }

    /**
     * Obtain the class type of the field annotated with {@link DocumentId}.
     * 
     * @param fieldId
     *            the {@link Field} annotated with {@link DocumentId}.
     * @return the field class type
     */
    private Class<?> getFieldIdClassType(java.lang.reflect.Field fieldId) {
        Class<?> fieldType = fieldId.getType();

        if (fieldType.isPrimitive())
            throw new ObjectParseException(
                "The type of a DocumentId field can not be primitive. Change the type of the field: " + fieldId);
        return fieldType;
    }

    /**
     * Obtain the Field Id value.
     * 
     * @param fieldId
     *            FieldId
     * @param document
     *            the Document
     * @return value of the field id
     */
    private Object getFieldIdValue(java.lang.reflect.Field fieldId, Document document) {

        Class<?> fieldType = getFieldIdClassType(fieldId);

        if (String.class.equals(fieldType))
            return document.getId();
        if (Long.class.equals(fieldType))
            return Long.valueOf(document.getId());
        if (Integer.class.equals(fieldType))
            return Integer.valueOf(document.getId());

        throw new ObjectParseException("A DocumentId must be String, Long or Integer.");
    }

    /**
     * Parses a {@link Document} to an {@link Object}.
     * 
     * @param document
     *            the {@link Document} to be parsed
     * @param classOfObj
     *            the base class of the object
     * @return an object with the given type
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
	<T> T parseObject(Document document, Class<T> classOfObj) throws InstantiationException, IllegalAccessException {

        T instanceOfT = classOfObj.newInstance();

        // the ID value
        java.lang.reflect.Field fieldId = ReflectionUtils.getAnnotatedField(classOfObj, DocumentId.class);
        fieldId.set(instanceOfT, getFieldIdValue(fieldId, document));

        // others values
        List<java.lang.reflect.Field> fields = ReflectionUtils.getAnnotatedFields(classOfObj, DocumentField.class);
        
        /**
         *  James Huang Add any collection annotations
         */
        fields.addAll( ReflectionUtils.getAnnotatedFields(classOfObj, DocumentCollection.class));

        for (java.lang.reflect.Field f : fields) {
            Object value = getDocumentFieldValue(document, f);
            //Check that if Field f is an enum to use the enum.valueof Method
            System.out.println("value:[" + value + "]");
            System.out.println("f.getType():[" + f.getType() + "]");
            System.out.println("f.getType().isEnum():[" + f.getType().isEnum() + "]" );
   
            				
            if( value != null && f.getType().isEnum()  ){ // if value == null there is no need to convert the value to an enum
            	
            	if( value instanceof String){ 
                    System.out.println("(String) value:[" + (String) value + "]");
            		if( value.equals("null")){ // "null" edge case
            			f.set(instanceOfT, null); // set the field to null
            		}
            		else{
            			System.out.println("Enum.valueOf( (Class<Enum>) f.getType() , (String) value)[" + Enum.valueOf( (Class<Enum>) f.getType() , (String) value));
            			f.set(instanceOfT, Enum.valueOf( (Class<Enum>) f.getType() , (String) value) );
            		}
            		
            	}
            	else{
            		throw new IllegalArgumentException("Doco parseObject() ERROR field f is enum but value is not string, f.getType().isEnum()[" + f.getType().isEnum() +  "]" + ", value.getClass()[" + value.getClass() + "]");
            	}
            	
            }
            else{
            	f.set(instanceOfT, value);
            }
            
        }

        return instanceOfT;
    }
    
    /**
     * initializes the concrete collection specified by the DocumentCollectionType Enum
     * @param type is the DocumentCollectionType
     * @return
     */
    @SuppressWarnings("rawtypes")
	public static Collection getConcreteCollection( DocumentCollectionType type){
//    	ARRAYLIST, LINKEDLIST, VECTOR, STACK, ARRAY_DEQUE, HASHSET, LINKED_HASHSET, TREESET, PRIORITY_QUEUE
    	if( type == DocumentCollectionType.ARRAYLIST){
    		return new ArrayList();
    	}
    	else if( type == DocumentCollectionType.LINKEDLIST ){
    		return new LinkedList();
    	}
    	else if( type == DocumentCollectionType.VECTOR ){
    		return new Vector();
    	}
    	else if( type == DocumentCollectionType.STACK ){
    		return new Stack();
    	}
    	else if( type == DocumentCollectionType.ARRAY_DEQUE ){
    		return new ArrayDeque();
    	}
		else if( type == DocumentCollectionType.HASHSET ){
			return new HashSet();
		}
		else if( type == DocumentCollectionType.LINKED_HASHSET ){
			return new LinkedHashSet();
		}
		else if( type == DocumentCollectionType.TREESET ){
			return new TreeSet();
		}
		else if( type == DocumentCollectionType.PRIORITY_QUEUE ){
			return new PriorityQueue();
		}
    	else{
    		throw new IllegalArgumentException(" INVALID DocumentCollectionType: " + type);
    	}
    	
    }

}