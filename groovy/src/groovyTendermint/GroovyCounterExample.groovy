package groovyTendermint


import java.nio.ByteBuffer

import com.github.jtendermint.jabci.api.ICheckTx
import com.github.jtendermint.jabci.api.ICommit
import com.github.jtendermint.jabci.api.IDeliverTx
import com.github.jtendermint.jabci.socket.TSocket
import com.github.jtendermint.jabci.types.Types.CodeType
import com.github.jtendermint.jabci.types.Types.RequestCheckTx
import com.github.jtendermint.jabci.types.Types.RequestCommit
import com.github.jtendermint.jabci.types.Types.RequestDeliverTx
import com.github.jtendermint.jabci.types.Types.ResponseCheckTx
import com.github.jtendermint.jabci.types.Types.ResponseCommit
import com.github.jtendermint.jabci.types.Types.ResponseDeliverTx
import com.google.protobuf.ByteString

import groovy.transform.Field


@Field def hashCount = 0
@Field def txCount = 0
@Field def socket = new TSocket()

socket.registerListener([requestCheckTx: {tx -> return checktx(tx)}] as ICheckTx)

socket.registerListener([receivedDeliverTx: {tx -> return receivedDeliverTx(tx)}] as IDeliverTx)

socket.registerListener([requestCommit: {tx -> return requestCommit(tx)}] as ICommit)



println "connecting $socket"
new Thread(socket.start())

while(true) {
    Thread.sleep(2000)
}



ResponseCheckTx checktx(RequestCheckTx req) {
    println "got check tx"
    ByteString tx = req.getTx()
    if (tx.size() <= 4) {
        int txCheck = new BigInteger(1, tx.toByteArray()).intValueExact()
        if (txCheck < txCount) {
            println "txcheck is smaller than txCount, got $txCheck and $txCount"
            return ResponseCheckTx.newBuilder().setCode(CodeType.BadNonce).setLog("tx-value is smaller than tx-count").build()
        }
    }

    System.out.println("SENDING OK")
    return ResponseCheckTx.newBuilder().setCode(CodeType.OK).build()
}


ResponseDeliverTx receivedDeliverTx(RequestDeliverTx req) {

    println "got deliver tx"

    ByteString tx = req.getTx()

    socket.printByteArray(tx.toByteArray())

    if (tx.size() == 0) {
        return ResponseDeliverTx.newBuilder().setCode(CodeType.BadNonce).setLog("transaction is empty").build()
    } else if (tx.size() <= 4) {
        int x = new BigInteger(1, tx.toByteArray()).intValueExact()
    } else {
        return ResponseDeliverTx.newBuilder().setCode(CodeType.BadNonce).setLog("got a bad value").build()
    }

    txCount += 1
    println "TX Count is now: $txCount"
    return ResponseDeliverTx.newBuilder().setCode(CodeType.OK).build()
}

ResponseCommit requestCommit(RequestCommit requestCommit) {
    hashCount += 1

    if (txCount == 0) {
        return ResponseCommit.newBuilder().setCode(CodeType.OK).build()
    } else {
        ByteBuffer buf = ByteBuffer.allocate(Integer.SIZE)
        buf.putInt(txCount)
        return ResponseCommit.newBuilder().setCode(CodeType.OK).setData(ByteString.copyFrom(buf)).build()
    }
}
