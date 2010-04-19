/**
 * Copyright 2010 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.view.webapp.component.cvlazypopup;

import org.primefaces.event.CloseEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.model.CvObject;
import uk.ac.ebi.intact.view.webapp.controller.application.CvObjectService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Component
@Scope("request")
public class CvLazyPopupController {

    private Map<String, CvObject> cvObjectMap;
    private boolean dialogRendered;

    private TransactionStatus transactionStatus;


    @Autowired
    private DataContext dataContext;

    @Autowired
    private CvObjectService cvObjectService;

    public CvLazyPopupController() {
        this.cvObjectMap = new HashMap<String, CvObject>();
    }

    @PostConstruct
    public void initTransaction() {
        transactionStatus = dataContext.beginTransaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @PreDestroy
    public void endTransaction() {
        dataContext.rollbackTransaction(transactionStatus);
    }

    public CvObject fetch(String className, String identifier) {
       return cvObjectService.loadByIdentifier(className, identifier);
    }

    public void closeDialog(CloseEvent evt) {
        dialogRendered = false;
    }

    public boolean isDialogRendered() {
        return dialogRendered;
    }

    public void setDialogRendered(boolean dialogRendered) {
        this.dialogRendered = dialogRendered;
    }
}