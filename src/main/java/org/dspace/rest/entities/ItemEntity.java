/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rest.entities;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.content.authority.Choices;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.rest.util.UserRequestParams;
import org.sakaiproject.entitybus.EntityReference;
import org.sakaiproject.entitybus.exception.EntityException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class ItemEntity extends ItemEntityTrim {

    private Object collection;

    public ItemEntity() {
    }

    public ItemEntity(String uid, Context context, UserRequestParams uparams) throws SQLException {
        super(uid, context);

        boolean collections = uparams.getCollections();
        boolean trim = uparams.getTrim();

        this.collection = collections ? trim ? new CollectionEntityTrimC(res.getOwningCollection()) : new CollectionEntityC(res.getOwningCollection())
                : new CollectionEntityId(res.getOwningCollection());
    }

    public ItemEntity(Item item, UserRequestParams uparams) throws SQLException {
        super(item);

        boolean collections = uparams.getCollections();
        boolean trim = uparams.getTrim();

        this.collection = collections ? trim ? new CollectionEntityTrimC(item.getOwningCollection()) : new CollectionEntityC(item.getOwningCollection())
                : new CollectionEntityId(item.getOwningCollection());
    }

    public Object getMetadataFields(EntityReference ref, UserRequestParams uparams, Context context) {
        try {
            List<Object> entities = new ArrayList<Object>();

            MetadataField[] fields = MetadataField.findAll(context);
            for (MetadataField field : fields) {
                int id = field.getFieldID();
                MetadataSchema schema = MetadataSchema.find(context, field.getSchemaID());
                String name = schema.getName() + "." + field.getElement();
                if (field.getQualifier() != null) {
                    name += "." + field.getQualifier();
                }
                entities.add(new MetadataFieldEntity(id, name));
            }
            return entities;
        } catch (SQLException e) {
            throw new EntityException("Internal server error", "SQL error", 500);
        }
    }

    public String createMetadata(EntityReference ref, Map<String, Object> inputVar, Context context) {

        try {
            Item item = Item.find(context, Integer.parseInt(ref.getId()));

            AuthorizeManager.authorizeAction(context, item, Constants.WRITE);

            Integer id = Integer.parseInt((String) inputVar.get("id"));
            String value = (String) inputVar.get("value");
            String lang = (String) inputVar.get("lang");

            MetadataField field = MetadataField.find(context, Integer.valueOf(id));
            MetadataSchema schema = MetadataSchema.find(context, field.getSchemaID());

            item.addMetadata(schema.getName(), field.getElement(), field.getQualifier(), lang, value);
            item.update();

            return String.valueOf(item.getID());
        } catch (SQLException ex) {
            throw new EntityException("Internal server error", "SQL error", 500);
        } catch (AuthorizeException ae) {
            throw new EntityException("Forbidden", "Forbidden", 403);
        } catch (NumberFormatException ex) {
            throw new EntityException("Bad request", "Could not parse input", 400);
        }
    }

    public void editMetadata(EntityReference ref, Map<String, Object> inputVar, Context context) {

        try {
            Item item = Item.find(context, Integer.parseInt(ref.getId()));

            AuthorizeManager.authorizeAction(context, item, Constants.WRITE);
            item.clearMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            List<Map> fieldList = new ArrayList<Map>();
            Object o = inputVar.get("metadata");
            if (o instanceof Map) {
                Map map = (Map) o;
                fieldList.add((Map) map.get("field"));
            } else if (o instanceof Vector) {
                Vector vector = (Vector) o;
                for (int i = 0; i < vector.size(); i++) {
                    fieldList.add((Map) vector.get(i));
                }
            }

            for (Map fieldMap : fieldList) {
                String name = (String) fieldMap.get("name");
                String value = (String) fieldMap.get("value");
                String authority = (String) fieldMap.get("authority");
                String confidence = (String) fieldMap.get("confidence");
                String lang = (String) fieldMap.get("lang");

                // get the field's name broken up
                String[] parts = parseName(name);
                // probe for a confidence value
                int iconf = Choices.CF_UNSET;
                if (confidence != null && confidence.length() > 0) {
                    iconf = Choices.getConfidenceValue(confidence);
                }
                // upgrade to a minimum of NOVALUE if there IS an authority key
                if (authority != null && authority.length() > 0 && iconf == Choices.CF_UNSET) {
                    iconf = Choices.CF_NOVALUE;
                }
                item.addMetadata(parts[0], parts[1], parts[2], lang, value, authority, iconf);
            }
            item.update();
        } catch (SQLException ex) {
            throw new EntityException("Internal server error", "SQL error", 500);
        } catch (AuthorizeException ae) {
            throw new EntityException("Forbidden", "Forbidden", 403);
        } catch (NumberFormatException ex) {
            throw new EntityException("Bad request", "Could not parse input", 400);
        }
    }

    public void removeMetadata(EntityReference ref, Map<String, Object> inputVar, Context context) {
        try {
            Integer id = Integer.parseInt((String) inputVar.get("id"));
            Item item = Item.find(context, Integer.parseInt(ref.getId()));

            AuthorizeManager.authorizeAction(context, item, Constants.WRITE);

            Integer eid = Integer.parseInt((String) inputVar.get("eid"));
            MetadataValue metadataValue = MetadataValue.find(context, eid);
            if (metadataValue != null && metadataValue.getItemId() == id) {
                metadataValue.delete(context);
            } else {
                throw new EntityException("Internal server error", "No such metadata value or not belongs to same item", 500);
            }
        } catch (SQLException ex) {
            throw new EntityException("Internal server error", "SQL error", 500);
        } catch (AuthorizeException ae) {
            throw new EntityException("Forbidden", "Forbidden", 403);
        } catch (NumberFormatException ex) {
            throw new EntityException("Bad request", "Could not parse input", 400);
        } catch (IOException ie) {
            throw new EntityException("Internal server error", "SQL error, cannot remove metadata value", 500);
        }
    }

    public Object getCommentsCount(EntityReference ref, UserRequestParams uparams, Context context) {
        try {
            return Comment.countItems(context, Integer.parseInt(ref.getId()));
        } catch (SQLException ex) {
            throw new EntityException("Internal server error", "SQL error", 500);
        }
    }

    public Object getComments(EntityReference ref, UserRequestParams uparams, Context context) {
        try {
            List<Object> entities = new ArrayList<Object>();

            Comment[] comments = Comment.findAllTop(context, Integer.parseInt(ref.getId()), uparams.getStart(), uparams.getLimit());
            for (Comment comment : comments) {
                entities.add(new CommentEntity(comment));
            }
            return entities;
        } catch (SQLException e) {
            throw new EntityException("Internal server error", "SQL error", 500);
        }
    }

    public Object getRating(EntityReference ref, UserRequestParams uparams, Context context) {
        try {
            int itemId = Integer.parseInt(ref.getId());
            if (context.getCurrentUser() != null) {
                Rating rating = Rating.find(context, itemId, context.getCurrentUser().getID());
                return rating == null ? new RatingEntityId() : new RatingEntity(rating);
            }
            return 0;
        } catch (SQLException e) {
            throw new EntityException("Internal server error", "SQL error", 500);
        } catch (NumberFormatException ex) {
            throw new EntityException("Bad request", "Could not parse input", 400);
        }
    }

    public Object getBitstreams(EntityReference ref, UserRequestParams uparams, Context context) {

        try {
            Item res = Item.find(context, Integer.parseInt(ref.getId()));

            AuthorizeManager.authorizeAction(context, res, Constants.READ);

            List<Object> entities = new ArrayList<Object>();
            Bitstream[] bst = res.getNonInternalBitstreams();

            for (Bitstream b : bst) {
                entities.add(new BitstreamEntity(b));
            }
            return entities;
        } catch (SQLException ex) {
            throw new EntityException("Internal server error", "SQL error", 500);
        } catch (AuthorizeException ex) {
            throw new EntityException("Forbidden", "Forbidden", 403);
        } catch (NumberFormatException ex) {
            throw new EntityException("Bad request", "Could not parse input", 400);
        }
    }

    private static String[] parseName(String name) {
        String[] parts = new String[3];

        String[] split = name.split("\\.");
        if (split.length == 2) {
            parts[0] = split[0];
            parts[1] = split[1];
            parts[2] = null;
        } else if (split.length == 3) {
            parts[0] = split[0];
            parts[1] = split[1];
            parts[2] = split[2];
        } else {
            throw new EntityException("Bad request", "Could not parse input", 400);
        }
        return parts;
    }

    public Object getCollection() {
        return collection;
    }
}