/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:26
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 18:18
 */

package org.novid20.sdk.core

internal class DispatchersImpl : Dispatchers {
    override val io = kotlinx.coroutines.Dispatchers.IO
    override val main = kotlinx.coroutines.Dispatchers.Main
    override val default = kotlinx.coroutines.Dispatchers.Default
}