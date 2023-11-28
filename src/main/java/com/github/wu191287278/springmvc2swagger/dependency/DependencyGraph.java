package com.github.wu191287278.springmvc2swagger.dependency;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * @author shenglin.li  2022/10/9 14:50
 * @version 1.0
 */
public class DependencyGraph {
    private Map<String, GraphNode> nodeIndex = new HashMap();
    private Set<GraphNode> root = new HashSet<>();

    public void addDependency(String nodeName, String dependency) {
        GraphNode p = nodeIndex.compute(nodeName, (k, v) -> v == null ? new GraphNode(k) : v);
        GraphNode c = nodeIndex.compute(dependency, (k, v) -> v == null ? new GraphNode(k) : v);
        if(c.inChild(p)) { // 循环引用则放弃
            return;
        }

        p.addDependency(c);
        if (p.haveParent()) {
            root.remove(p);
        } else {
            root.add(p);
        }
    }

    public void visit(BiConsumer<String, String> consumer) {
        root.forEach(e -> e.visit(consumer));
    }
}
