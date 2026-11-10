package com.expeditee.plant.identifier.classifier

data class Prediction(
    val node: Node,
    val probability: Double,
    val rank: Float,
) {
    constructor(
        secondNode: Node,
        d: Double,
    ) : this(
        node = secondNode,
        probability = d,
        rank = secondNode.rank ?: 0F,
    )
}
