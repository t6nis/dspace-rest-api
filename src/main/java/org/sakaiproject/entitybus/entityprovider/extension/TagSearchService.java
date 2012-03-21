/**
 * $Id: TagSearchService.java 17 2009-02-17 12:33:06Z azeckoski $
 * $URL: http://entitybus.googlecode.com/svn/tags/entitybus-1.0.8/api/src/main/java/org/sakaiproject/entitybus/entityprovider/extension/TagSearchService.java $
 * TagSearchProvider.java - entity-broker - Apr 5, 2008 7:21:20 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sakaiproject.entitybus.entityprovider.extension;

import java.util.List;

/**
 * Defines the methods necessary for searching for entities by tags (shared interface)
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public interface TagSearchService extends TagProvider {

    /**
     * Search for all entities with a set of tags, prefixes, and a search
     * 
     * @param tags a set of tags defined on these entities which should match
     * @param prefixes (optional) limit the search to a given set of prefixes
     * @param matchAll if true then all tags must be matched, else find entities with any tags in the set given
     * @param search (optional) search params which are used to limit the return (paging and max/limit)
     * @return a list of entity data objects representing all found entities
     */
    public List<EntityData> findEntitesByTags(String[] tags, String[] prefixes,
            boolean matchAll, org.sakaiproject.entitybus.entityprovider.search.Search search);

}