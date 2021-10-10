package com.jelurida.ardor.client.api;

import isoRaw.isoRaw;
import isoRaw.VoteWeighting;
import isoRaw.addons.JO;
import isoRaw.http.callers.GetBlockCall;
import isoRaw.http.callers.SendMoneyCall;
import isoRaw.http.responses.BlockResponse;
import isoRaw.http.responses.TransactionResponse;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Sample Java program which demonstrates how to submit a phased transaction
 */
public class PhasedTransaction {

    private static final String SECRET_PHRASE = "hope peace happen touch easy pretend worthless talk them indeed wheel state";

    public static void main(String[] args) throws MalformedURLException {
        URL url = new URL("https://testisoRaw.com/isoRaw");

        PhasedTransaction phasedTransaction = new PhasedTransaction();
        phasedTransaction.submitPhasedTransaction(url);
    }

    private void submitPhasedTransaction(URL url) {
        JO block = GetBlockCall.create().remote(url).call();
        BlockResponse blockResponse = BlockResponse.create(block);
        int height = blockResponse.getHeight();

        JO signedTransactionResponse = SendMoneyCall.create().
                recipient("isoRaw-KX2S-UULA-7YZ7-F3R8L").
                amountNQT(12345678).
                secretPhrase(SECRET_PHRASE).
                deadline(15).
                feeNQT(200000000).
                phased(true).
                phasingVotingModel(VoteWeighting.VotingModel.ACCOUNT.getCode()). // Another account will need to approve this
                phasingQuorum(1). // One approver account is enough
                phasingWhitelisted("isoRaw-EVHD-5FLM-3NMQ-G46NR"). // This is the account that needs to approve
                phasingFinishHeight(height + 100). // It has 100 blocks to submit the approval
                phasingMinBalanceModel(VoteWeighting.MinBalanceModel.NONE.getCode()). // There is no minimum balance requirement
                remote(url).
                call();

        System.out.printf("SendMoney response: %s\n", signedTransactionResponse.toJSONString());
        TransactionResponse transactionResponse = TransactionResponse.create(signedTransactionResponse.getJo("transactionJSON"));
        System.out.printf("Phased: %s\n", transactionResponse.isPhased());
    }

}
