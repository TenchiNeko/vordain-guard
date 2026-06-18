package com.vordain.guard.vpn.classifier

import com.vordain.guard.core.model.DomainClassification
import com.vordain.guard.core.model.DomainName

interface DomainClassifier {
    fun classify(domain: DomainName): DomainClassification
}
