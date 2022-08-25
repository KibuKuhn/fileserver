package kibu.kuhn.fileserver.service;

import java.util.Comparator;

import kibu.kuhn.fileserver.domain.Node;

class NodeComparator implements Comparator<Node> {

	@Override
	public int compare(Node o1, Node o2) {
		if (o1.isFolder() && o2.isFolder() || !o1.isFolder() && !o2.isFolder()) {
			return o1.getTitle().compareToIgnoreCase(o2.getTitle());
		}
		
		return o1.isFolder() ? -1 : 1;
	}	
}