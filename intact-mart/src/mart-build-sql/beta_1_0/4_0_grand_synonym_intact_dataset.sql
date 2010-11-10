-- ###############################################################################################
-- #                      create all the grands
-- #      SELECT 'GRANT SELECT ON '||table_name||' TO INTACT_SELECT;'
-- #      FROM user_tables WHERE table_name LIKE 'INTACT__%';
-- ###############################################################################################


GRANT SELECT ON INTACT__COMPONENT_ALIAS__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__COMPONENT_ANNO__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__COMPONENT_XREF__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__COMPONENT__MAIN TO INTACT_SELECT;
GRANT SELECT ON INTACT__EXPERIMENT_ALIAS__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__EXPERIMENT_ANNO__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__EXPERIMENT_XREF__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__EXPERIMENT__MAIN TO INTACT_SELECT;
GRANT SELECT ON INTACT__FEATURE_ALIAS__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__FEATURE_ANNO__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__FEATURE_XREF__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__FEATURE__MAIN TO INTACT_SELECT;
GRANT SELECT ON INTACT__HOSTORG_CELLTYPE__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__HOSTORG_TISSUE__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__INTERACTION_ALIAS__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__INTERACTION_ANNO__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__INTERACTION_OWNER__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__INTERACTION_XREF__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__INTERACTION__MAIN TO INTACT_SELECT;
GRANT SELECT ON INTACT__INTERACTOR_ALIAS__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__INTERACTOR_ANNO__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__INTERACTOR_XREF__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__INTERACTOR__MAIN TO INTACT_SELECT;
GRANT SELECT ON INTACT__PUBLICATION_ALIAS__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__PUBLICATION_ANNO__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__PUBLICATION_XREF__DM TO INTACT_SELECT;
GRANT SELECT ON INTACT__PUBLICATION__MAIN TO INTACT_SELECT;
GRANT SELECT ON INTACT__RANGE__DM TO INTACT_SELECT;







-- ###############################################################################################
-- #                      create all the synonyms
-- #   SELECT 'CREATE OR REPLACE PUBLIC SYNONYM '||table_name||' FOR INTACT.' || table_name || ';'
-- #   FROM user_tables WHERE table_name LIKE 'INTACT__%';
-- ###############################################################################################


CREATE OR REPLACE PUBLIC SYNONYM INTACT__COMPONENT_ALIAS__DM FOR INTACT.INTACT__COMPONENT_ALIAS__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__COMPONENT_ANNO__DM FOR INTACT.INTACT__COMPONENT_ANNO__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__COMPONENT_XREF__DM FOR INTACT.INTACT__COMPONENT_XREF__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__COMPONENT__MAIN FOR INTACT.INTACT__COMPONENT__MAIN;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__EXPERIMENT_ALIAS__DM FOR INTACT.INTACT__EXPERIMENT_ALIAS__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__EXPERIMENT_ANNO__DM FOR INTACT.INTACT__EXPERIMENT_ANNO__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__EXPERIMENT_XREF__DM FOR INTACT.INTACT__EXPERIMENT_XREF__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__EXPERIMENT__MAIN FOR INTACT.INTACT__EXPERIMENT__MAIN;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__FEATURE_ALIAS__DM FOR INTACT.INTACT__FEATURE_ALIAS__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__FEATURE_ANNO__DM FOR INTACT.INTACT__FEATURE_ANNO__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__FEATURE_XREF__DM FOR INTACT.INTACT__FEATURE_XREF__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__FEATURE__MAIN FOR INTACT.INTACT__FEATURE__MAIN;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__HOSTORG_CELLTYPE__DM FOR INTACT.INTACT__HOSTORG_CELLTYPE__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__HOSTORG_TISSUE__DM FOR INTACT.INTACT__HOSTORG_TISSUE__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__INTERACTION_ALIAS__DM FOR INTACT.INTACT__INTERACTION_ALIAS__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__INTERACTION_ANNO__DM FOR INTACT.INTACT__INTERACTION_ANNO__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__INTERACTION_OWNER__DM FOR INTACT.INTACT__INTERACTION_OWNER__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__INTERACTION_XREF__DM FOR INTACT.INTACT__INTERACTION_XREF__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__INTERACTION__MAIN FOR INTACT.INTACT__INTERACTION__MAIN;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__INTERACTOR_ALIAS__DM FOR INTACT.INTACT__INTERACTOR_ALIAS__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__INTERACTOR_ANNO__DM FOR INTACT.INTACT__INTERACTOR_ANNO__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__INTERACTOR_XREF__DM FOR INTACT.INTACT__INTERACTOR_XREF__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__INTERACTOR__MAIN FOR INTACT.INTACT__INTERACTOR__MAIN;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__PUBLICATION_ALIAS__DM FOR INTACT.INTACT__PUBLICATION_ALIAS__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__PUBLICATION_ANNO__DM FOR INTACT.INTACT__PUBLICATION_ANNO__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__PUBLICATION_XREF__DM FOR INTACT.INTACT__PUBLICATION_XREF__DM;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__PUBLICATION__MAIN FOR INTACT.INTACT__PUBLICATION__MAIN;
CREATE OR REPLACE PUBLIC SYNONYM INTACT__RANGE__DM FOR INTACT.INTACT__RANGE__DM;