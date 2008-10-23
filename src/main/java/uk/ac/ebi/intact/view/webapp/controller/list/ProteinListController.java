/**
 * Copyright 2008 The European Bioinformatics Institute, and others.
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
package uk.ac.ebi.intact.view.webapp.controller.list;


import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.apache.myfaces.trinidad.render.ExtendedRenderKitService;
import org.apache.myfaces.trinidad.util.Service;
import org.apache.myfaces.trinidad.context.RequestContext;
import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.intact.psimitab.IntactBinaryInteraction;
import uk.ac.ebi.intact.view.webapp.controller.BaseController;
import uk.ac.ebi.intact.view.webapp.controller.search.SearchController;
import uk.ac.ebi.intact.view.webapp.util.ExternalDbLinker;

import javax.faces.event.ActionEvent;
import javax.faces.context.FacesContext;
import java.util.List;
import java.util.Set;

/**
 * Controller for ProteinList View
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
public class ProteinListController extends BaseController {

    public ProteinListController() {
    }

    private String[] getSelectedUniprotIds() {

        final List<IntactBinaryInteraction> interactions = getSelected( SearchController.PROTEINS_TABLE_ID );
        Set<String> uniprotIds = ExternalDbLinker.getUniqueUniprotIds( interactions );
        return uniprotIds.toArray( new String[uniprotIds.size()] );
    }


    private String[] getSelectedGeneNames() {

        final List<IntactBinaryInteraction> interactions = getSelected( SearchController.PROTEINS_TABLE_ID );
        Set<String> geneNames = ExternalDbLinker.getUniqueGeneNames( interactions );
        return geneNames.toArray( new String[geneNames.size()] );
    }


    public void goDomains( ActionEvent evt ) {
        String[] selectedUniprotIds = getSelectedUniprotIds();
        ExternalDbLinker.goExternalLink( ExternalDbLinker.INTERPROURL, ExternalDbLinker.INTERPRO_SEPERATOR, selectedUniprotIds );
    }

    public void goExpression( ActionEvent evt ) {
        String[] selectedGeneNames = getSelectedGeneNames();
        ExternalDbLinker.goExternalLink( ExternalDbLinker.EXPRESSIONURL_PREFIX, ExternalDbLinker.EXPRESSIONURL_SUFFIX, ExternalDbLinker.EXPRESSION_SEPERATOR, selectedGeneNames );
    }

    public void goChromosomalLocation( ActionEvent evt ) {
        String[] selectedUniprotIds = getSelectedUniprotIds();
        ExternalDbLinker.goExternalLink( ExternalDbLinker.CHROMOSOMEURL, ExternalDbLinker.CHROMOSOME_SEPERATOR, selectedUniprotIds );
    }


    public void goReactome( ActionEvent evt ) {
        String[] selected = getSelectedUniprotIds();
        //the carriage return has to be escaped as it is used in the JavaScript
        ExternalDbLinker.reactomeLinker( ExternalDbLinker.REACTOMEURL, "\\r", selected );

    }

    public void rerender(ActionEvent evt) {
        RequestContext requestContext = RequestContext.getCurrentInstance();
        requestContext.addPartialTarget(getComponentFromView("buttonBar"));
    }
}
