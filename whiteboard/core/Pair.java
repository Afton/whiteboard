package whiteboard.core;

public class Pair<T, E> {
	private final T first;
	private final E second;
	
	public Pair(final T first, final E second) {
		this.first = first;
		this.second = second;
	}
	
	public T getFirst() {
		return first;
	}
	
	public E getSecond() {
		return second;
	}
	
	public boolean equals(Pair<T,E> p)
	{
		if (p == null)
			return false;
		return this.first.equals(p.getFirst()) && this.second.equals(p.getSecond());
	}
	
	public int hashCode()
	{
		int hash = 1;
		hash  = hash *31 + ((this.first == null)? 0 : this.first.hashCode());
		hash = hash * 31 + ((this.second == null)? 0 : this.second.hashCode());
		return hash;
	}
}