package com.vidolima.doco;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Facet;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.GeoPoint;
import com.google.common.base.Strings;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Ref;
import com.vidolima.doco.annotation.DocumentCollection;
import com.vidolima.doco.annotation.DocumentEmbed;
import com.vidolima.doco.annotation.DocumentField;
import com.vidolima.doco.annotation.DocumentId;
import com.vidolima.doco.annotation.DocumentRef;
import com.vidolima.doco.annotation.FacetField;
import com.vidolima.doco.annotation.FieldType;
import com.vidolima.doco.exception.DocumentParseException;

/**
 * This class is used to create {@link Document}s instances from Objects.
 * 
 * @author Marcos Alexandre Vidolin de Lima
 * @since January 28, 2014
 */
final class DocumentParser {

    private static final String DEFAULT_FIELD_NAME_PREFIX = "";
    
    private ObjectifyFactory objectifyFactory = new ObjectifyFactory();
    
    /**
     * Obtains the {@link java.lang.reflect.Field} annotated with {@link DocumentId} annotation.
     * 
     * @param classOfObj
     *            the class of the object that contains the field
     * @return the {@link java.lang.reflect.Field} that contains the {@link DocumentId} annotation
     * @exception DocumentParseException
     */
    private java.lang.reflect.Field getDocumentIdField(Class<?> classOfObj) throws DocumentParseException {
        List<java.lang.reflect.Field> result = ReflectionUtils.getAnnotatedFields(classOfObj, DocumentId.class);
        
        if (result.size() > 1) {
            throw new DocumentParseException("More than one occurrence of @DocumentId found in " + classOfObj);
        }

        if (result.isEmpty()) {
            throw new DocumentParseException("No @DocumentId annotation was found in " + classOfObj);
        }

        return result.get(0);
    }

    /**
     * Obtains all the {@link java.lang.reflect.Field}s annotated with the {@link DocumentId}.
     * 
     * @param classOfObj
     *            the class of the object that contains the fields to be search
     * @return all the annotated {@link java.lang.reflect.Field} that contains the {@link DocumentField} annotation.
     */
    List<java.lang.reflect.Field> getAllDocumentField(Class<?> classOfObj) {
        return ReflectionUtils.getAnnotatedFields(classOfObj, DocumentField.class);
    }

    private List<java.lang.reflect.Field> getAllRefFields(Class<?> classOfObj) {
        return ReflectionUtils.getAnnotatedFields(classOfObj, DocumentRef.class);
    }

    private List<java.lang.reflect.Field> getAllEmbedFields(Class<?> classOfObj) {
        return ReflectionUtils.getAnnotatedFields(classOfObj, DocumentEmbed.class);
    }

    List<java.lang.reflect.Field> getAllFacetFields(Class<?> classOfObj) {
        return ReflectionUtils.getAnnotatedFields(classOfObj, FacetField.class);
    }
    
    /**
     * Obtains all the {@link java.lang.reflect.Field}s annotated with {@link DocumentCollection}
     * @author James Huang
     */
    List<java.lang.reflect.Field> getAllDocumentCollection(Class<?> classOfObj){
    	return ReflectionUtils.getAnnotatedFields(classOfObj, DocumentCollection.class);
    }

    /**
     * Returns the {@link DocumentId} value of a given Object.
     * 
     * @param obj
     *            the object base
     * @param classOfObj
     *            the class of object
     * @return the id of the document
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> T getId(Object obj, Class<?> classOfObj, Class classOfT) throws IllegalArgumentException,
        IllegalAccessException {

        java.lang.reflect.Field field = getDocumentIdField(classOfObj);
        T id = (T) ReflectionUtils.getFieldValue(field, obj, classOfT);

        if (id == null) {
        	//if fieldType is Long auto generate and Id
        	Key<?> autoId = objectifyFactory.allocateId(classOfObj);
        	if(field.getType().isAssignableFrom(String.class)){ // check if field type is Long or String
        		id = (T) (autoId.getId() + "");
        	}
        	else if(field.getType().isAssignableFrom(Long.class) ){
        		id = (T) new Long( autoId.getId());
        	}
        	else{
        		throw new DocumentParseException("No id was set to \"" + field.getName() + "\" field in " + classOfObj + ", and could not autoGenerate because ID Field type is not String or Long"); 
        	}
            
        }

        return id;
    }

    /**
     * Obtains a {@link com.google.appengine.api.search.Field} given a name, value and type.
     * 
     * @param fieldValue
     *            the value to be set to the returned field
     * @param name
     *            the name of returned field
     * @param field
     *            the {@link java.lang.reflect.Field}
     * @param obj
     *            the object base
     * 
     * @return com.google.appengine.api.search.Field
     */
    private com.google.appengine.api.search.Field getSearchNumberField(String name, java.lang.reflect.Field field,
        Object fieldValue) {

        if (fieldValue == null)
            return null;

        if (Integer.TYPE.equals(field.getType()) || Integer.class.equals(field.getType())) {
            Integer number = (Integer) fieldValue;
            return Field.newBuilder().setName(name).setNumber(number).build();
        }
        if (Long.TYPE.equals(field.getType()) || Long.class.equals(field.getType())) {
            Long number = (Long) fieldValue;
            return Field.newBuilder().setName(name).setNumber(number).build();
        }
        if (Float.TYPE.equals(field.getType()) || Float.class.equals(field.getType())) {
            Float number = (Float) fieldValue;
            return Field.newBuilder().setName(name).setNumber(number).build();
        }
        if (Double.TYPE.equals(field.getType()) || Double.class.equals(field.getType())) {
            Double number = (Double) fieldValue;
            return Field.newBuilder().setName(name).setNumber(number).build();
        }

        throw new DocumentParseException(
            "A DocumentField typed as NUMBER must be Long, Integer, Float or Double. Field '" + field.getName()
                + "' is incompatible.");
    }

    /**
     * Obtains a List of {@link com.google.appengine.api.search.Field} given a name, value and type.
     * Note by James Huang: Modified to return a list of fields if using @DocumentCollection annotation
     * 						if the field is instanceof Collection.class the List will have more than 1 field in the return list,
     * 						otherwise the return List of fields will only have 1 field
     * @param fieldValue
     *            the value to be set to the returned field
     * @param name
     *            the name of returned field
     * @param field
     *            the {@link java.lang.reflect.Field}
     * @param obj
     *            the object base
     * @return the  List<{@link com.google.appengine.api.search.Field}>
     * @throws IllegalAccessException
     */
    @SuppressWarnings("rawtypes")
	private List<com.google.appengine.api.search.Field> getSearchFieldByFieldType(String name, java.lang.reflect.Field field,
        Object obj, FieldType fieldType) throws IllegalAccessException {

    	List<com.google.appengine.api.search.Field> fieldsToReturn = new ArrayList<>();
    	
        Object fieldValue = field.get(obj); // gets the value of field from obj
        
        if (FieldType.TEXT.equals(fieldType)) {
        	//Check if the fieldValue is a Collection
        	//if it is add each item in the Collection as a separate field with the same name but different value
        	if( fieldValue instanceof Collection ){
        		
				Collection col = (Collection) fieldValue;
				Iterator colIterator = col.iterator(); 
        		while( colIterator.hasNext() ){
        			Object text = colIterator.next();
        			if( text instanceof String){ // multi-value fields can only be Strings, not Date of Number
        				fieldsToReturn.add( Field.newBuilder().setName(name).setText((String) text).build() ); // we know text is a String so we can add it to the collection
        			}
        			else{
//        				logger.warn("getSearchFieldByFieldType(), fieldValue is instanceof List but is not List<String>:" + fieldValue );
        				throw new IllegalArgumentException("getSearchFieldByFieldType(), fieldValue is instanceof List but is not List<String>:" + fieldValue);
        			}
        			
        		}
        	}
        	else{
        		String text = String.valueOf(fieldValue);
        		fieldsToReturn.add( Field.newBuilder().setName(name).setText(text).build() );
        	}
            
        }
        if ( FieldType.HTML.equals(fieldType)) {
        	//Check if the fieldValue is a Collection
        	//if it is add each item in the Collection as a separate field with the same name but different value
        	if( fieldValue instanceof Collection ){
        		Collection col = (Collection) fieldValue;
        		Iterator colIterator = col.iterator();
        		while( colIterator.hasNext() ){
        			Object html = colIterator.next();
        			if( html instanceof String){ // multi-value fields can only be Strings not Date of Number
        				fieldsToReturn.add( Field.newBuilder().setName(name).setHTML( (String) html).build() );
        			}
        			else{
//        				logger.warn("getSearchFieldByFieldType(), fieldValue is instanceof List but is not List<String>:" + fieldValue );
        				throw new IllegalArgumentException("getSearchFieldByFieldType(), fieldValue is instanceof List but is not List<String>:" + fieldValue);
        			}
        		}
        	}
        	else{
	            String html = String.valueOf(fieldValue);
	            fieldsToReturn.add( Field.newBuilder().setName(name).setHTML(html).build() );
        	}
        }
        if (FieldType.ATOM.equals(fieldType)) {
        	
        	if( fieldValue instanceof Collection ){
        		Collection col = (Collection) fieldValue;
        		Iterator colIterator = col.iterator();
        		while( colIterator.hasNext() ){
        			Object atom = colIterator.next();
        			if( atom instanceof String){ // multi-value fields can only be Strings not Date or Number
        				fieldsToReturn.add( Field.newBuilder().setName(name).setAtom( (String) atom).build() );
        			}
        			else{
//        				logger.warn("getSearchFieldByFieldType(), fieldValue is instanceof List but is not List<String>:" + fieldValue );
        				throw new IllegalArgumentException("getSearchFieldByFieldType(), fieldValue is instanceof List but is not List<String>:" + fieldValue);
        			}
        			
        		}
        	}
        	else{
        		String atom = String.valueOf(fieldValue);
        		fieldsToReturn.add( Field.newBuilder().setName(name).setAtom(atom).build() );
        	}
            
        }
        if (FieldType.DATE.equals(fieldType)) {
            if (fieldValue != null) {
                Date date = (Date) fieldValue;
                fieldsToReturn.add( Field.newBuilder().setName(name).setDate(date).build() );
            }
        }
        if (FieldType.GEO_POINT.equals(fieldType)) {
            if (fieldValue != null) {
                if (fieldValue instanceof GeoPt) {
                    GeoPt geoPt = (GeoPt) fieldValue;
                    GeoPoint geoPoint = new GeoPoint(geoPt.getLatitude(), geoPt.getLongitude());
                    fieldsToReturn.add( Field.newBuilder().setName(name).setGeoPoint(geoPoint).build() );
                } else {
                    GeoPoint geoPoint = (GeoPoint) fieldValue;
                    fieldsToReturn.add( Field.newBuilder().setName(name).setGeoPoint(geoPoint).build() );
                }
            }
        }
        if (FieldType.NUMBER.equals(fieldType))
        	fieldsToReturn.add( getSearchNumberField(name, field, fieldValue) );

        // Note: When you create a document you must specify all of its
        // attributes using the Document.Builder class method. You cannot add,
        // remove, or delete fields, nor change the identifier or any other
        // attribute once the document has been created. Date and geopoint
        // fields must be assigned a non-null value. Atom, text, HTML, and
        // number fields can be empty
        return fieldsToReturn; // return null;
    }

    /**
     * Obtains a list of {@link com.google.appengine.api.search.Field} from {@link FieldType}.
     * NOTE: James Huang Modified to also check for all {@link DocumentCollections}
     * @param obj
     *            the object base
     * @param classOfObj
     *            the class of object
     * @return a list of {@link com.google.appengine.api.search.Field}
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    List<com.google.appengine.api.search.Field> getAllSearchFieldsByType(String fieldNamePrefix, Object obj,
        Class<?> classOfObj, FieldType fieldType) throws IllegalArgumentException, IllegalAccessException {

        List<com.google.appengine.api.search.Field> fields = new ArrayList<Field>(0);

        for (java.lang.reflect.Field f : getAllDocumentField(classOfObj)) { //iterate through every @DocumentField
        	
            // TODO: validate field (declaration of annotation)
            DocumentField annotation = ObjectParser.getDocumentFieldAnnotation(f);

            if (annotation.type().equals(fieldType)) { //check if the type is the FieldType we are looking for
                String name = ObjectParser.getFieldNameValue(f, annotation);
                String fullName = Strings.isNullOrEmpty(fieldNamePrefix) ? name : fieldNamePrefix + "_" + name;
                //Gets all the Search Fields from the object field
                //if the object field is a Collection(i.e. a List) we have to store it as a multi-valued field
                //TODO: A DocumentCollection is no longer a DocumentField but its own annotation. Maybe check if we can delete some of this code
                List<com.google.appengine.api.search.Field> searchFields = getSearchFieldByFieldType(fullName, f, obj, fieldType);
                for( com.google.appengine.api.search.Field field: searchFields){
                	if (field != null) {
                        fields.add(field);
                    }
                }
                
            }
        }
        
        /**
         * James Huang
         */
        for( java.lang.reflect.Field f: getAllDocumentCollection(classOfObj)){ // iterate through every @DocumentCollection
        	DocumentCollection annotation = ObjectParser.getDocumentCollectionAnnotation(f);
        	if( annotation.fieldType().equals(fieldType) ) { // check if it is the FieldType we are looking for
        		String name = ObjectParser.getFieldNameValue(f, annotation);
        		String fullName = Strings.isNullOrEmpty(fieldNamePrefix) ? name : fieldNamePrefix + "_" + name;
        		
        		List<com.google.appengine.api.search.Field> searchFields = getSearchFieldByFieldType(fullName, f, obj, fieldType);
        		for( com.google.appengine.api.search.Field field: searchFields){
                	if (field != null) {
                        fields.add(field);
                    }
                }
        		
        	}
        }

        return fields;
    }

    /**
     * Obtains all fields with Doco annotations.
     * 
     * @param obj
     *            the origin object
     * @param classOfObj
     *            the class of obj
     * @return a list of {@link com.google.appengine.api.search.Field}
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    List<com.google.appengine.api.search.Field> getAllSearchFields(String fieldNamePrefix, Object obj,
        Class<?> classOfObj) throws IllegalArgumentException, IllegalAccessException {

        List<com.google.appengine.api.search.Field> fields = new ArrayList<com.google.appengine.api.search.Field>();

        for (FieldType type : FieldType.values()) { // loops through every FieldType (i.e. TEXT, ATOM, HTML, DATE, NUMBER, GEOPOINT )
            for (com.google.appengine.api.search.Field f : getAllSearchFieldsByType(fieldNamePrefix, obj, classOfObj,
                type)) {
                fields.add(f);
            }
        }

        return fields;
    }

    /**
     * Parses a object to an {@link Document}.
     * 
     * @param obj
     *            the object to be parsed
     * @param documentId
     *            user defined id of document (e.g. 'Key' of a datastore entity).
     * @param typeOfObj
     *            the base class of the given object
     * @return a {@link Document}
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    Document parseDocument(Object obj, String documentId, Class<?> classOfObj) throws IllegalArgumentException,
        IllegalAccessException {
        String id = documentId;
        if (id == null) {
            java.lang.reflect.Field field = getDocumentIdField(classOfObj);
            id = String.valueOf(getId(obj, classOfObj, field.getType()));
        }

        Document.Builder builder = Document.newBuilder().setId(id);

        for (com.google.appengine.api.search.Field f : getAllFieldsForDocument(DEFAULT_FIELD_NAME_PREFIX, obj,
            classOfObj)) {
            if (f != null) {
                builder.addField(f);
            }
        }

        for (Facet facet : getAllFacetsForDocument(obj, classOfObj)) {
            builder.addFacet(facet);
        }

        return builder.build();
    }

    private List<Facet> getAllFacetsForDocument(Object obj, Class<?> classOfObj) throws IllegalArgumentException,
        IllegalAccessException {
        List<Facet> facetsInClass = new ArrayList<>();
        for (java.lang.reflect.Field f : getAllFacetFields(classOfObj)) {
            Facet facet = getFacetValueFromField(f, obj);
            facetsInClass.add(facet);
        }
        return facetsInClass;
    }

    private Facet getFacetValueFromField(java.lang.reflect.Field f, Object obj) throws IllegalArgumentException,
        IllegalAccessException {
        FacetField annotation = f.getAnnotation(FacetField.class);
        String facetName = StringUtils.isNotBlank(annotation.name()) ? annotation.name() : f.getName();
        switch (annotation.type()) {
        case ATOM:
            return Facet.withAtom(facetName, String.valueOf(f.get(obj)));
        case NUMBER:
            return Facet.withNumber(facetName, Double.valueOf(String.valueOf(f.get(obj))));
        default:
            throw new IllegalStateException(String.format("Unknown facet type %s found", facetName));
        }
    }

    /**
     * Parses class for presence of @DocumentField, @DocumentRef, etc. annotations and generates {@link Field} from it.
     * 
     * @param fieldNamePrefix
     *            prefix for field names if any. Normally this should be set to empty string or null. When supplied,
     *            name of field becomes <b>prefix_</b>fieldName
     * @param obj
     *            object which should be used to get value of document fields.
     * @param classOfObj
     *            class of 'obj' parameter
     * @return All possible fields which are to be added to the search document including fields of Ref entity.
     */
    private List<com.google.appengine.api.search.Field> getAllFieldsForDocument(String fieldNamePrefix, Object obj,
        Class<?> classOfObj) throws IllegalArgumentException, IllegalAccessException {
        List<com.google.appengine.api.search.Field> eligibleFields = new ArrayList<>();
        // get fields annotated with @DocumentField
        for (com.google.appengine.api.search.Field f : getAllSearchFields(fieldNamePrefix, obj, classOfObj)) {
            if (f != null) {
                eligibleFields.add(f);
            }
        }
        // get fields annotated with @DocumentRef
        for (com.google.appengine.api.search.Field f : getAllSearchFieldsFromRefClass(fieldNamePrefix, obj, classOfObj)) {
            if (f != null) {
                eligibleFields.add(f);
            }
        }
        // get fields annotated with @DocumentEmbed
        for (com.google.appengine.api.search.Field f : getAllSearchFieldsInEmbedClass(fieldNamePrefix, obj, classOfObj)) {
            if (f != null) {
                eligibleFields.add(f);
            }
        }
        return eligibleFields;
    }

    private List<com.google.appengine.api.search.Field> getAllSearchFieldsInEmbedClass(String fieldNamePrefix,
        Object obj, Class<?> classOfObj) throws IllegalArgumentException, IllegalAccessException {
        List<com.google.appengine.api.search.Field> searchFields = new ArrayList<com.google.appengine.api.search.Field>();
        List<java.lang.reflect.Field> declaredFields = getAllEmbedFields(classOfObj);
        for (java.lang.reflect.Field declaredField : declaredFields) {
            Object fieldValue = declaredField.get(obj);
            if (fieldValue != null) {
                String newFieldNamePrefix = Strings.isNullOrEmpty(fieldNamePrefix) ? declaredField.getType()
                    .getSimpleName() : (fieldNamePrefix + "_" + declaredField.getType().getSimpleName());
                searchFields.addAll(getAllFieldsForDocument(newFieldNamePrefix, fieldValue, declaredField.getType()));
            }
        }
        return searchFields;
    }

    /**
     * Creates {@link Field} for all annotated fields in the class of field annotated with {@link DocumentRef}
     */
    private List<com.google.appengine.api.search.Field> getAllSearchFieldsFromRefClass(String fieldNamePrefix,
        Object obj, Class<?> classOfObj) throws IllegalArgumentException, IllegalAccessException {
        List<com.google.appengine.api.search.Field> searchFields = new ArrayList<com.google.appengine.api.search.Field>();
        List<java.lang.reflect.Field> declaredFields = getAllRefFields(classOfObj);
        for (java.lang.reflect.Field declaredField : declaredFields) {
            Object fieldValue = declaredField.get(obj);
            // get the class from the annotation on this field (guaranteed to be annotated since we specifically asked
            // for annotated fields only)
            Class<?> refClass = declaredField.getAnnotation(DocumentRef.class).type();
            if (!(fieldValue instanceof Ref<?>)) {
                throw new IllegalStateException("Incorrect mapping found on field: " + declaredField.getName());
            }
            Ref<?> ref = (Ref<?>) fieldValue;
            String newFieldNamePrefix = Strings.isNullOrEmpty(fieldNamePrefix) ? refClass.getSimpleName()
                : fieldNamePrefix + "_" + refClass.getSimpleName();
            searchFields.addAll(getAllFieldsForDocument(newFieldNamePrefix, ref.get(), refClass));
        }
        return searchFields;
    }
}