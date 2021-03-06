/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.json.resource.PatchOperation.*;
import static org.forgerock.json.resource.Requests.*;
import static org.forgerock.json.resource.Resources.newInternalConnection;
import static org.forgerock.json.resource.TestUtils.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.testng.Assert.fail;

import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.util.query.QueryFilter;

/**
 * Tests for {@link MemoryBackend}.
 */
@SuppressWarnings("javadoc")
public final class MemoryBackendTest {

    @Test
    public void testActionCollectionClear() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", userAlice()));
        connection.create(ctx(), newCreateRequest("users", userBob()));
        connection.action(ctx(), newActionRequest("users", "clear"));
        try {
            connection.read(ctx(), newReadRequest("users/0"));
            fail("Read succeeded unexpectedly");
        } catch (final Exception e) {
            assertThat(e).isInstanceOf(NotFoundException.class);
        }
        try {
            connection.read(ctx(), newReadRequest("users/1"));
            fail("Read succeeded unexpectedly");
        } catch (final Exception e) {
            assertThat(e).isInstanceOf(NotFoundException.class);
        }
    }

    @Test(expectedExceptions = NotSupportedException.class)
    public void testActionCollectionUnknown() throws Exception {
        final Connection connection = getConnection();
        connection.action(ctx(), newActionRequest("users", "unknown"));
    }

    @Test(expectedExceptions = NotSupportedException.class)
    public void testActionInstanceUnknown() throws Exception {
        final Connection connection = getConnection();
        connection.action(ctx(), newActionRequest("users/0", "unknown"));
    }

    @Test
    public void testCreateCollection() throws Exception {
        final Connection connection = getConnection();
        final Resource resource1 =
                connection.create(ctx(), newCreateRequest("users", userAlice()));
        final Resource resource2 = connection.read(ctx(), newReadRequest("users/0"));
        assertThat(resource1).isEqualTo(resource2);
        assertThat(resource1.getId()).isEqualTo("0");
        assertThat(resource1.getRevision()).isEqualTo("0");
        assertThat(resource1.getContent().getObject()).isEqualTo(
                userAliceWithIdAndRev(0, 0).getObject());
    }

    @Test
    public void testCreateInstance() throws Exception {
        final Connection connection = getConnection();
        final Resource resource1 =
                connection.create(ctx(), newCreateRequest("users", "123", userAlice()));
        final Resource resource2 = connection.read(ctx(), newReadRequest("users/123"));
        assertThat(resource1).isEqualTo(resource2);
        assertThat(resource1.getId()).isEqualTo("123");
        assertThat(resource1.getRevision()).isEqualTo("0");
        assertThat(resource1.getContent().getObject()).isEqualTo(
                userAliceWithIdAndRev(123, 0).getObject());
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testDeleteCollection() throws Exception {
        final Connection connection = getConnection();
        connection.delete(ctx(), newDeleteRequest("users"));
    }

    @Test
    public void testDeleteInstance() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", userAlice()));
        final Resource resource = connection.delete(ctx(), newDeleteRequest("users/0"));
        assertThat(resource.getId()).isEqualTo("0");
        assertThat(resource.getRevision()).isEqualTo("0");
        assertThat(resource.getContent().getObject()).isEqualTo(
                userAliceWithIdAndRev(0, 0).getObject());
        try {
            connection.read(ctx(), newReadRequest("users/0"));
            fail("Read succeeded unexpectedly");
        } catch (final NotFoundException e) {
            // Expected.
        }
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testPatchCollection() throws Exception {
        final Connection connection = getConnection();
        connection.patch(ctx(), newPatchRequest("users", add("/test", "value")));
    }

    @Test
    public void testPatchInstance() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", userAlice()));
        final Resource resource =
                connection.patch(ctx(), newPatchRequest("users/0", replace("/name", "bob"),
                        increment("/age", 10), remove("/role"), add("/role", "it")));
        assertThat(resource.getId()).isEqualTo("0");
        assertThat(resource.getRevision()).isEqualTo("1");
        assertThat(resource.getContent().getObject()).isEqualTo(
                userBobWithIdAndRev(0, 1).getObject());
    }

    @Test
    public void testQueryCollection() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", userAlice()));
        connection.create(ctx(), newCreateRequest("users", userBob()));
        final Collection<Resource> results = new ArrayList<Resource>();
        connection.query(ctx(), newQueryRequest("users"), results);
        assertThat(results).containsOnly(asResource(userAliceWithIdAndRev(0, 0)),
                asResource(userBobWithIdAndRev(1, 0)));
    }

    @Test
    public void testQueryCollectionWithFilters() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", userAlice()));
        connection.create(ctx(), newCreateRequest("users", userBob()));
        final Collection<Resource> results = new ArrayList<Resource>();
        connection.query(ctx(), newQueryRequest("users").setQueryFilter(
                QueryFilter.equalTo(new JsonPointer("name"), "alice")).addField("_id"), results);
        assertThat(results).hasSize(1);
        final Resource resource = results.iterator().next();
        assertThat(resource.getId()).isEqualTo("0");
        assertThat(resource.getRevision()).isEqualTo("0");
        assertThat(resource.getContent().getObject()).isEqualTo(object(field("_id", "0")));
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testQueryInstance() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", userAlice()));
        connection.create(ctx(), newCreateRequest("users", userBob()));
        final Collection<Resource> results = new ArrayList<Resource>();
        connection.query(ctx(), newQueryRequest("users/0"), results);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testReadCollection() throws Exception {
        final Connection connection = getConnection();
        connection.read(ctx(), newReadRequest("users"));
    }

    @Test
    public void testReadInstance() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", userAlice()));
        final Resource resource = connection.read(ctx(), newReadRequest("users/0"));
        assertThat(resource.getId()).isEqualTo("0");
        assertThat(resource.getRevision()).isEqualTo("0");
        assertThat(resource.getContent().getObject()).isEqualTo(
                userAliceWithIdAndRev(0, 0).getObject());
    }

    @Test
    public void testReadInstanceWithFieldFilter() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", userAlice()));
        final Resource resource =
                connection.read(ctx(), newReadRequest("users/0").addField("_id"));
        assertThat(resource.getId()).isEqualTo("0");
        assertThat(resource.getRevision()).isEqualTo("0");
        assertThat(resource.getContent()).stringAt("_id").isEqualTo("0");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testUpdateCollection() throws Exception {
        final Connection connection = getConnection();
        connection.update(ctx(), newUpdateRequest("users", userAlice()));
    }

    @Test
    public void testUpdateInstance() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", userAlice()));
        final Resource resource = connection.update(ctx(), newUpdateRequest("users/0", userBob()));
        assertThat(resource.getId()).isEqualTo("0");
        assertThat(resource.getRevision()).isEqualTo("1");
        assertThat(resource.getContent().getObject()).isEqualTo(
                userBobWithIdAndRev(0, 1).getObject());
    }

    private Connection getConnection() {
        final MemoryBackend users = new MemoryBackend();
        final UriRouter router = new UriRouter();
        router.addRoute("users", users);
        return newInternalConnection(router);
    }

    private JsonValue userAlice() {
        return content(object(field("name", "alice"), field("age", 20), field("role", "sales")));
    }

    private JsonValue userAliceWithIdAndRev(final int id, final int rev) {
        return content(object(field("name", "alice"), field("age", 20), field("role", "sales"),
                field("_id", String.valueOf(id)), field("_rev", String.valueOf(rev))));
    }

    private JsonValue userBob() {
        return content(object(field("name", "bob"), field("age", 30), field("role", "it")));
    }

    private JsonValue userBobWithIdAndRev(final int id, final int rev) {
        return content(object(field("name", "bob"), field("age", 30), field("role", "it"), field(
                "_id", String.valueOf(id)), field("_rev", String.valueOf(rev))));
    }

}
