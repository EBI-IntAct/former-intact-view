package uk.ac.ebi.intact.editor.controller.curate.organism;

import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.controller.curate.cvobject.CvObjectService;
import uk.ac.ebi.intact.model.AnnotatedObject;
import uk.ac.ebi.intact.model.BioSource;

import javax.faces.event.ComponentSystemEvent;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "general" )
public class BioSourceController extends AnnotatedObjectController {

    @Autowired
    private CvObjectService cvObjectService;

    private String ac;
    private BioSource bioSource;

    private boolean isTopic;

    @Override
    public AnnotatedObject getAnnotatedObject() {
        return bioSource;
    }

    @Override
    public void setAnnotatedObject(AnnotatedObject annotatedObject) {
       this.bioSource = (BioSource) annotatedObject;
    }

    public void loadData(ComponentSystemEvent evt) {
        if (ac != null) {
            bioSource = loadByAc(IntactContext.getCurrentInstance().getDaoFactory().getBioSourceDao(), ac);
        } else {
            bioSource = new BioSource();
        }

        generalLoadChecks();
    }

    public String newOrganism() {
        BioSource bioSource = new BioSource();
        setBioSource(bioSource);

        //getUnsavedChangeManager().markAsUnsaved(bioSource);

        return navigateToObject(bioSource);
    }

    public String getAc() {
        return ac;
    }

    public void setAc(String ac) {
        this.ac = ac;
    }

    public BioSource getBioSource() {
        return bioSource;
    }

    public void setBioSource(BioSource bioSource) {
        this.bioSource = bioSource;
        this.ac = bioSource.getAc();
    }


}