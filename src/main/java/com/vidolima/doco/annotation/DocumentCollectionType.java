package com.vidolima.doco.annotation;

/**
 * Defines all the valid Collection Types supported by DocumentCollection annotation.
 * Place one of these types in a {@link DocumentCollection} annotation to indicate the Collection type
 * 
 * @author James Huang
 *
 */
//TODO: Make sures all these types are supported
public enum DocumentCollectionType {
	ARRAYLIST, LINKEDLIST, VECTOR, STACK, ARRAY_DEQUE, HASHSET, LINKED_HASHSET, TREESET, PRIORITY_QUEUE
}
