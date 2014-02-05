package com.vidolima.doco;

import java.lang.reflect.Field;
import java.util.List;

import com.google.appengine.api.search.Document;
import com.vidolima.doco.annotation.DocumentField;
import com.vidolima.doco.annotation.DocumentId;
import com.vidolima.doco.annotation.FieldType;

/**
 * This class is used to create objects instances from {@link Document}s.
 * 
 * @author Marcos Alexandre Vidolin de Lima
 * @since January 26, 2014
 */
final class ObjectParser {

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
		if (name == null || name.isEmpty()) {
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
	 * Obtains the value of the document field given a {@link Document} and a
	 * field name.
	 * 
	 * @param document
	 *            the {@link Document} that contains the field
	 * @param fieldName
	 *            the name of the field to get the value
	 * @return the value of the field
	 */
	private Object getDocumentFieldValue(Document document, String fieldName) {

		com.google.appengine.api.search.Field field = document
				.getOnlyField(fieldName);

		com.google.appengine.api.search.Field.FieldType fieldType = field
				.getType();

		if (com.google.appengine.api.search.Field.FieldType.TEXT
				.equals(fieldType))
			return field.getText();
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
			return document.getOnlyField(fieldName).getNumber();
		else if (com.google.appengine.api.search.Field.FieldType.GEO_POINT
				.equals(fieldType))
			return document.getOnlyField(fieldName).getGeoPoint();

		// TODO: exception?
		return null;
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
		fieldId.set(instanceOfT, document.getId());

		// others values
		List<java.lang.reflect.Field> fields = ReflectionUtils
				.getAnnotatedFields(classOfObj, DocumentField.class);

		for (java.lang.reflect.Field f : fields) {

			DocumentField annotation = ObjectParser
					.getDocumentFieldAnnotation(f);

			String fieldName = ObjectParser.getFieldNameValue(f, annotation);
			Object value = getDocumentFieldValue(document, fieldName);

			f.set(instanceOfT, value);
		}

		return instanceOfT;
	}

}