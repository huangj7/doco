package com.vidolima.doco;

import java.lang.reflect.Field;
import java.util.List;

import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Index;
import com.vidolima.doco.annotation.DocumentField;
import com.vidolima.doco.annotation.DocumentId;
import com.vidolima.doco.annotation.DocumentIndex;
import com.vidolima.doco.annotation.FieldType;
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

		if (annotation == null)
			throw new AnnotationNotFoundException(
					"There is no @DocumentIndex annotation declared in "
							+ clazz + " class.");

		String name = annotation.name();
		if (name != null && name.length() > 0) {
			return name;
		}

		return clazz.getSimpleName();
	}

	/**
	 * Obtains the name value of the {@link DocumentField} annotation of a given
	 * {@link Field} or the name of the {@link Field} by default.
	 * 
	 * @param field
	 * @param annotation
	 * @return the specified parameter name of the {@link DocumentField}
	 *         annotation, or if not specified returns the name of the field by
	 *         default
	 */
	static String getFieldNameValue(java.lang.reflect.Field field,
			DocumentField annotation) {
		String name = annotation.name();
		if (name == null || name.trim().length() == 0) {
			name = field.getName();
		}
		return name;
	}

	/**
	 * Obtains the {@link FieldType} of the {@link DocumentField} annotation of
	 * a given {@link Field}.
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
	static DocumentField getDocumentFieldAnnotation(
			java.lang.reflect.Field field) {
		return field.getAnnotation(DocumentField.class);
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
	private Object getDocumentFieldNumberValue(Document document,
			java.lang.reflect.Field field, String fieldName) {

		Class<?> fieldType = field.getType();

		if (Integer.class.equals(fieldType))
			return Integer.valueOf(document.getOnlyField(fieldName).getNumber()
					.intValue());
		if (Long.class.equals(fieldType))
			return Long.valueOf(document.getOnlyField(fieldName).getNumber()
					.longValue());
		if (Float.class.equals(fieldType))
			return Float.valueOf(document.getOnlyField(fieldName).getNumber()
					.floatValue());
		if (Double.class.equals(fieldType))
			return document.getOnlyField(fieldName).getNumber();

		throw new ObjectParseException(
				"A DocumentField typed as NUMBER must be Long, Integer, Float or Double.");
	}

	/**
	 * Obtains the value of the document field given a {@link Document} and a
	 * field name.
	 * 
	 * @param document
	 *            the {@link Document} that contains the field
	 * @param field
	 *            the field to get the value
	 * @return the value of the field
	 */
	private Object getDocumentFieldValue(Document document,
			java.lang.reflect.Field field) {

		DocumentField annotation = getDocumentFieldAnnotation(field);

		String fieldName = getFieldNameValue(field, annotation);

		com.google.appengine.api.search.Field f = document
				.getOnlyField(fieldName);

		com.google.appengine.api.search.Field.FieldType fieldType = f.getType();

		if (com.google.appengine.api.search.Field.FieldType.TEXT
				.equals(fieldType))
			return f.getText();
		else if (com.google.appengine.api.search.Field.FieldType.ATOM
				.equals(fieldType))
			return document.getOnlyField(fieldName).getAtom();
		else if (com.google.appengine.api.search.Field.FieldType.HTML
				.equals(fieldType))
			return document.getOnlyField(fieldName).getHTML();
		else if (com.google.appengine.api.search.Field.FieldType.DATE
				.equals(fieldType))
			return document.getOnlyField(fieldName).getDate();
		else if (com.google.appengine.api.search.Field.FieldType.NUMBER
				.equals(fieldType))
			return getDocumentFieldNumberValue(document, field, fieldName);
		else if (com.google.appengine.api.search.Field.FieldType.GEO_POINT
				.equals(fieldType))
			return document.getOnlyField(fieldName).getGeoPoint();

		throw new IllegalAnnotationDeclarationException(
				"Invalid com.google.appengine.api.search.Field.FieldType: "
						+ fieldType);
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
			if (fieldType.isPrimitive())
				throw new ObjectParseException(
						"The type of a DocumentId field can not be primitive. Change the type of the field: "
								+ fieldId);
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
	private Object getFieldIdValue(java.lang.reflect.Field fieldId,
			Document document) {

		Class<?> fieldType = getFieldIdClassType(fieldId);

		if (String.class.equals(fieldType))
			return document.getId();
		if (Long.class.equals(fieldType))
			return Long.valueOf(document.getId());
		if (Integer.class.equals(fieldType))
			return Integer.valueOf(document.getId());

		throw new ObjectParseException(
				"A DocumentId must be String, Long or Integer.");
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
	<T> T parseObject(Document document, Class<T> classOfObj)
			throws InstantiationException, IllegalAccessException {

		T instanceOfT = classOfObj.newInstance();

		// the ID value
		java.lang.reflect.Field fieldId = ReflectionUtils.getAnnotatedField(
				classOfObj, DocumentId.class);
		fieldId.set(instanceOfT, getFieldIdValue(fieldId, document));

		// others values
		List<java.lang.reflect.Field> fields = ReflectionUtils
				.getAnnotatedFields(classOfObj, DocumentField.class);

		for (java.lang.reflect.Field f : fields) {
			Object value = getDocumentFieldValue(document, f);
			f.set(instanceOfT, value);
		}

		return instanceOfT;
	}

}