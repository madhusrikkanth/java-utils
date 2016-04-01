package com.mssrikkanth.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class LinkedTreeSet<E> implements Set<E> {

	private static class Node<S> implements Comparable<Node<S>>{
		public Node<S> previousNode = null;
		public Node<S> nextNode = null;
		public Object element = null;
		
		public Node(){}
		
		public Node(S val){
			element = val;
		}
		
		public void setPreviousNode(Node<S> previous){
			previousNode = previous;
		}
		
		public void setNextNode(Node<S> next){
			nextNode = next;
		}
		
		@Override
		public boolean equals(Object obj){
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (obj instanceof Node){
				//the following cast will never fail as this is a private class
				@SuppressWarnings("unchecked")
				Node<S> nd = (Node<S>)obj; 
				return this.element.equals(nd.element);
			}
			return true;
		}
		
		@Override
		public int hashCode(){
			return element.hashCode();
		}
		
		@SuppressWarnings("unchecked")
		public int compareTo(Node<S> obj){
			//if S is not an instanceof Comparable this will throw a ClassCastException
			//as would be expected from any class in Collections framework
			//If it is instanceof Comparable then this cast will not fail
			Comparable<? super S> oE1 = (Comparable <? super S>) element;
			//This cast would succeed 
			//It may throw a ClassCastException when called from contains()
			//as is expected of that method
			S elem1 = (S)obj.element;
			return oE1.compareTo(elem1);
		}
		
		@SuppressWarnings("unchecked")
		public static<T> Comparator<Node<T>> getNodeComparator(Comparator<? super T> cmp){
			return new Comparator<Node<T>>(){
				public int compare(Node<T> obj1, Node<T> obj2){
					return cmp.compare((T)obj1.element, (T)obj2.element);
				}
			};
		}
	}
	
	private TreeSet<Node<E>> baseSet;
	private Node<E> head;
	private Node<E> tail;
	private Node<E> lastInserted;
	
	public LinkedTreeSet(){
		baseSet = new TreeSet<>();
		head = null;
		tail = null;
		lastInserted = null;
	}
	
	public LinkedTreeSet(Comparator<? super E> cmp){
		baseSet = new TreeSet<>(Node.<E>getNodeComparator(cmp));
	}
	
	@Override
	public int size(){
		return baseSet.size();
	}
	
	@Override
	public boolean isEmpty(){
		return baseSet.isEmpty();
	}
	
	@Override
	public boolean contains(Object o){
		Node<E> node = new Node<E>();
		node.element = o;
		return baseSet.contains(node);
	}
	
	//The iterator that traverses the list in the natural order
	//or the order specified by the comparator passed to the constructor
	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			private Iterator<Node<E>> iter = baseSet.iterator();
			private Node<E> lastReturned = null;
			public boolean hasNext(){
				return iter.hasNext();
			}
			
			public E next(){
				lastReturned = iter.next();
				@SuppressWarnings("unchecked")
				E elem = (E) lastReturned.element; //this cast won't fail
				return elem;
			}
			
			public void remove(){
				if (lastReturned == null)
					throw new IllegalStateException();
				Node<E> previous = lastReturned.previousNode;
				Node<E> next = lastReturned.nextNode;
				if (previous == null)
					head = next;
				else
					previous.setNextNode(next);
				if (next == null)
					tail = previous;
				else
					next.setPreviousNode(previous);
				iter.remove();
				lastReturned = null;
			}
		};
	}
	
	//The iterator that traverses the set in the order of insertion
	public Iterator<E> insOrderIterator(){
		return new Iterator<E>() {
			private Node<E> currNode = head;
			boolean nextCalled = false;		
			public boolean hasNext(){
				if (currNode == null)
					return false;
				return true;
			}
			
			public E next(){
				nextCalled = true;
				@SuppressWarnings("unchecked")
				E elem = (E)currNode.element; //this cast won't fail
				currNode = currNode.nextNode;
				return elem;
			}
			
			public void remove(){
				if (!nextCalled)
					throw new IllegalStateException();
				Node<E> toBeDeleted = null;
				if(currNode != null)
					toBeDeleted = currNode.previousNode;
				else 
					toBeDeleted = tail;
				Node<E> previous = null;
				if(toBeDeleted != head)
					previous = toBeDeleted.previousNode;
				Node<E> next = currNode;
				if (previous == null)
					head = next;
				else
					previous.setNextNode(next);
				if (next == null)
					tail = previous;
				else
					next.setPreviousNode(previous);
				baseSet.remove(toBeDeleted);
				nextCalled = false;
			}
		};
	}
	
	@Override
	public Object[] toArray(){
		Object[] arr = baseSet.toArray();
		Object[] ret = new Object[arr.length];
		int i = 0;
		for (Object obj:arr){
			@SuppressWarnings("unchecked")
			Node<E> tmp = (Node<E>) obj;
			ret[i++] = tmp.element;
		}
		return ret;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a){
		Object[] objArr = baseSet.toArray();
		int size = size();
        T[] r = a.length >= size ? a :
                  (T[])java.lang.reflect.Array
                  .newInstance(a.getClass().getComponentType(), size);
        int i = 0;
        for (Object obj: objArr){
        	Node<E> tmp = (Node<E>) obj;
        	r[i++] = (T)tmp.element;
        }
        if (r == a){
        	r[i] = null;
        }
        return r;
	}
	
	@Override
	public boolean add(E e){
		Node<E> newNode = new Node<E>(e);
		newNode.setPreviousNode(lastInserted);
		newNode.setNextNode(null);
		boolean ret = baseSet.add(newNode);
		if (ret){
			if (lastInserted != null)
				lastInserted.setNextNode(newNode);
			lastInserted = newNode;
			if (head == null)
				head = tail = newNode;
			else
				tail = newNode;
		}
		return ret;
	}
	
	@Override
	public boolean remove(Object o){
		boolean contains = contains(o);
		if(!contains) return false;
		@SuppressWarnings("unchecked")
		E val = (E)o; //this cast should not fail because of the contains check
		Node<E> node = new Node<E>(val);
		Node<E> treeNode = baseSet.floor(node);
		Node<E> previous = treeNode.previousNode;
		Node<E> next = treeNode.nextNode;
		if (previous == null)
			head = next;
		else
			previous.setNextNode(next);
		if (next == null)
			tail = previous;
		else
			next.setPreviousNode(previous);
		return baseSet.remove(treeNode);
	}
	
	@Override
	public boolean containsAll(Collection<?> c) {
        for (Object e : c)
            if (!contains(e))
                return false;
        return true;
    }
	
	@Override
	public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c)
            if (add(e))
                modified = true;
        return modified;
    }
	
	@Override
	public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        Iterator<?> it = iterator();
        while (it.hasNext()) {
            if (c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }
	
	@Override
	public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }
	
	@Override
	public void clear(){
		head = tail = lastInserted = null;
		baseSet.clear();
	}
}
