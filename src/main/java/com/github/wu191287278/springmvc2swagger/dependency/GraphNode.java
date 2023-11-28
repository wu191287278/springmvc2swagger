package com.github.wu191287278.springmvc2swagger.dependency;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * 图节点
 *
 * @author shenglin.li  2022/10/9 14:51
 * @version 1.0
 */
public class GraphNode {
    private String name;
    private Set<GraphNode> child = new HashSet<>();
    private Set<GraphNode> parent = new HashSet<>();

    public GraphNode(String name) {
        this.name = name;
    }

    public boolean addDependency(GraphNode node) {
        node.parent.add(this);
        return child.add(node);
    }
    public boolean haveParent() {
        return !parent.isEmpty();
    }
    public boolean haveChild() {
        return !child.isEmpty();
    }

    public boolean inChild(GraphNode node) {
        if(!haveChild()) {
            return false;
        }
        if(child.contains(node)) {
            return true;
        }
        return child.stream().anyMatch(e -> e.inChild(node));
    }

    public void visit(BiConsumer<String, String> consumer) {
        child.forEach(e -> {
            if(e.haveChild()) {
                e.visit(consumer);
            }
            consumer.accept(name, e.name);
        });
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return Objects.equals(name, ((GraphNode) obj).name);
    }
}
