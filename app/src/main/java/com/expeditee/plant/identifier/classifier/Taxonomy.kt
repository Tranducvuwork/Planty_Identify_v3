package com.expeditee.plant.identifier.classifier

import android.util.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

data class Taxonomy(
    val leaves: MutableList<Node> = mutableListOf(),
    var lifeNode: Node? = null,
    val nodeByKey: HashMap<String, Node> = hashMapOf(),
    val nodes: MutableList<Node> = mutableListOf(),
) {

    companion object {

        fun getTaxonomyFromInput(
            inputStream: InputStream,
        ): Taxonomy {
            val bufferedReader = BufferedReader(
                InputStreamReader(
                    inputStream,
                    Charset.forName("UTF-8"),
                ),
            )
            val taxonomy = Taxonomy()

            try {
                bufferedReader.readLine()

                while (true) {
                    val readLine = bufferedReader.readLine() ?: break
                    val node = Node.getNodeFromString(
                        readLine,
                    )
                    taxonomy.nodes.add(node)

                    if (node.leafId != null) {
                        if (node.leafId!!.isNotEmpty()) {
                            taxonomy.leaves.add(node)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(IdentificationClassifier.TAG, "Taxonomy parse error", e)
            }

            taxonomy.lifeNode = taxonomy.createLifeNode()

            if (taxonomy.nodes.toList().isNotEmpty()) {
                for (nextNode in taxonomy.nodes.toList()) {
                    taxonomy.nodeByKey[nextNode.key ?: ""] = nextNode
                }

                for (secondNextNode in taxonomy.nodes.toList()) {
                    if (secondNextNode.parentKey.isNullOrEmpty()) {
                        taxonomy.lifeNode!!.addChild(secondNextNode)
                    } else {
                        taxonomy.nodeByKey[secondNextNode.parentKey]?.addChild(
                            secondNextNode,
                        )
                    }
                }
            }

            return taxonomy
        }
    }

    private var filterByTaxonId: Int? = null
    private var isNegativeFilter: Boolean? = null
    private fun createLifeNode(): Node {
        val node = Node().apply {
            key = "48460"
            rank = 100.0F
            name = "Life"
            parent = null
        }
        return node
    }

    fun getModelSize(): Int {
        return this.leaves.size
    }

    fun predict(
        map: Map<Int, Any>,
    ): List<Prediction> {
        val twoDimFloatArray = map[0] as Array<*>
        val oneDimFloatArray = twoDimFloatArray[0] as FloatArray

        return buildBestBranchFromScores(
            aggregateScores(
                oneDimFloatArray,
            ),
        )
    }

    private fun aggregateScores(
        floatArray: FloatArray,
    ): Map<String, Float> {
        if (this.lifeNode != null) {
            return aggregateScores(
                floatArray,
                this.lifeNode!!,
            )
        }
        return emptyMap()
    }

    private fun aggregateScores(
        floatArray: FloatArray,
        node: Node,
    ): Map<String, Float> {
        var hasAncestor: Boolean? = null
        val hashMap = hashMapOf<String, Float>()
        var f = 0.0F

        if (node.children.toList().isNotEmpty()) {
            for (child in node.children.toList()) {
                hashMap.putAll(
                    aggregateScores(
                        floatArray = floatArray,
                        node = child,
                    ),
                )
            }

            for (child in node.children.toList()) {
                f += (hashMap[child.key] ?: 0.0F)
            }

            if (node.key != null) {
                hashMap[node.key!!] = f
            }
        } else {
            hasAncestor = if (this.filterByTaxonId != null) {
                !hasAncestor(
                    node,
                    this.filterByTaxonId.toString(),
                ) && this.isNegativeFilter == true
            } else {
                false
            }

            if (!hasAncestor && node.leafId != null) {
                f = floatArray[node.leafId!!.toInt()]
            }
            if (node.key != null) {
                hashMap[node.key!!] = f
            }
        }

        return hashMap
    }

    private fun hasAncestor(
        node: Node,
        string: String,
    ): Boolean {
        if (node.key != null) {
            if (node.key == string) {
                return true
            }
        }

        if (node.parent != null) {
            return hasAncestor(
                node.parent!!,
                string,
            )
        }

        return false
    }

    private fun buildBestBranchFromScores(
        map: Map<String, Float>,
    ): List<Prediction> {
        var node: Node? = null
        val predictions = mutableListOf<Prediction>()
        val secondNode = this.lifeNode

        if (secondNode != null) {
            predictions.add(
                Prediction(
                    secondNode = secondNode,
                    d = map[secondNode.key]?.toDouble() ?: 0.0,
                ),
            )

            if (secondNode.children.toList().isNotEmpty()) {
                var secondNodeChildren = secondNode.children.toList()

                while (secondNodeChildren.isNotEmpty()) {
                    node = null
                    var f = -1.0F

                    for (nextNode in secondNodeChildren) {
                        val floatValue = map[nextNode.key] ?: -1.0F

                        if (floatValue > f) {
                            node = nextNode
                            f = floatValue
                        }
                    }

                    if (node != null) {
                        predictions.add(
                            Prediction(
                                secondNode = node,
                                d = f.toDouble(),
                            ),
                        )
                    }

                    if (node != null) {
                        secondNodeChildren = node.children.toList()
                    }
                }
            }
        }

        return predictions.toList()
    }
}
