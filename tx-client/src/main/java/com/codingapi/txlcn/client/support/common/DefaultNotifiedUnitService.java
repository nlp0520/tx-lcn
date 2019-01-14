package com.codingapi.txlcn.client.support.common;

import com.codingapi.txlcn.client.bean.DTXLocal;
import com.codingapi.txlcn.client.support.common.template.TransactionCleanTemplate;
import com.codingapi.txlcn.client.message.helper.RpcExecuteService;
import com.codingapi.txlcn.client.message.helper.TransactionCmd;
import com.codingapi.txlcn.commons.exception.SerializerException;
import com.codingapi.txlcn.commons.exception.TransactionClearException;
import com.codingapi.txlcn.commons.exception.TxClientException;
import com.codingapi.txlcn.spi.message.params.NotifyUnitParams;
import com.codingapi.txlcn.commons.util.Transactions;
import com.codingapi.txlcn.logger.TxLogger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Description: 默认RPC命令业务
 * Date: 2018/12/20
 *
 * @author ujued
 */
public class DefaultNotifiedUnitService implements RpcExecuteService {

    private final TransactionCleanTemplate transactionCleanTemplate;

    @Autowired
    private TxLogger txLogger;


    public DefaultNotifiedUnitService(TransactionCleanTemplate transactionCleanTemplate) {
        this.transactionCleanTemplate = transactionCleanTemplate;
    }

    @Override
    public Object execute(TransactionCmd transactionCmd) throws TxClientException {
        try {
            NotifyUnitParams notifyUnitParams = transactionCmd.getMsg().loadData(NotifyUnitParams.class);
            // 保证业务线程执行完毕后执行事务清理操作
            if (DTXLocal.cur() != null) {
                synchronized (DTXLocal.cur()) {
                    txLogger.trace(transactionCmd.getGroupId(), notifyUnitParams.getUnitId(), Transactions.TAG_TRANSACTION,
                            "clean transaction cmd waiting for business code finish.");
                    DTXLocal.cur().wait();
                }
            }
            // 事务清理操作
            transactionCleanTemplate.clean(
                    notifyUnitParams.getGroupId(),
                    notifyUnitParams.getUnitId(),
                    notifyUnitParams.getUnitType(),
                    notifyUnitParams.getState());
            return null;
        } catch (SerializerException | TransactionClearException | InterruptedException e) {
            throw new TxClientException(e);
        }
    }
}