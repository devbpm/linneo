/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.devbpm.linneo.extension.supplychain;

/**
 * Keys of the data subjects added by the {@link SupplyChainTaxonomy}. Like fideslang's data subjects, they are flat:
 * none has a parent.
 *
 * @since 0.1.0
 */
public final class SupplyChainDataSubjects {

    /** Delivery Recipient: A person who receives or signs for a delivery, who may not be the customer. */
    public static final String DELIVERY_RECIPIENT = "delivery_recipient";

    /** Driver: A person who drives a vehicle carrying the organization's goods, as an employee or for a carrier. */
    public static final String DRIVER = "driver";

    /** Supplier Contact: A person who acts for a supplier organization, such as a sales representative. */
    public static final String SUPPLIER_CONTACT = "supplier_contact";

    private SupplyChainDataSubjects() {
    }
}
