/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.kogito.jobs.service.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.kie.kogito.jobs.api.Job;
import org.kie.kogito.jobs.service.model.ScheduledJob;
import org.kie.kogito.jobs.service.model.ScheduledJob.ScheduledJobBuilder;
import org.kie.kogito.jobs.service.model.job.JobDetails;
import org.kie.kogito.jobs.service.model.job.ScheduledJobAdapter;
import org.kie.kogito.jobs.service.repository.ReactiveJobRepository;
import org.kie.kogito.jobs.service.scheduler.impl.TimerDelegateJobScheduler;
import org.kie.kogito.jobs.service.validator.JobDetailsValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
@Path(JobResource.JOBS_PATH)
public class JobResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobResource.class);
    @SuppressWarnings("squid:S1075")
    public static final String JOBS_PATH = "/jobs";

    @Inject
    TimerDelegateJobScheduler scheduler;

    @Inject
    ReactiveJobRepository jobRepository;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<ScheduledJob> create(Job job) {
        LOGGER.debug("REST create {}", job);
        JobDetails jobDetails = JobDetailsValidator.validateToCreate(ScheduledJobAdapter.to(ScheduledJob.builder().job(job).build()));
        return Uni.createFrom().publisher(scheduler.schedule(jobDetails))
                .onItem().ifNull().failWith(new RuntimeException("Failed to schedule job " + job))
                .onItem().transform(ScheduledJobAdapter::of);
    }

    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<ScheduledJob> patch(@PathParam("id") String id, @RequestBody Job job) {
        LOGGER.debug("REST patch update {}", job);
        //validating allowed patch attributes
        JobDetails jobToBeMerged = JobDetailsValidator.validateToMerge(ScheduledJobAdapter.to(ScheduledJobBuilder.from(job)));
        return Uni.createFrom().publisher(scheduler.reschedule(id, jobToBeMerged.getTrigger()).buildRs())
                .onItem().ifNull().failWith(new NotFoundException("Failed to reschedule job " + job))
                .onItem().transform(ScheduledJobAdapter::of);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Uni<ScheduledJob> delete(@PathParam("id") String id) {
        return Uni.createFrom().completionStage(scheduler.cancel(id))
                .onItem().ifNull().failWith(new NotFoundException("Failed to cancel job scheduling for jobId " + id))
                .onItem().transform(ScheduledJobAdapter::of);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Uni<ScheduledJob> get(@PathParam("id") String id) {
        return Uni.createFrom().completionStage(jobRepository.get(id))
                .onItem().ifNull().failWith(new NotFoundException("Job not found id " + id))
                .onItem().transform(ScheduledJobAdapter::of);
    }
}
