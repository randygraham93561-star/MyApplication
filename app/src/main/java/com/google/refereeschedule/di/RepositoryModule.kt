package com.google.refereeschedule.di

import com.google.refereeschedule.data.repository.AssignmentRepositoryImpl
import com.google.refereeschedule.data.repository.GameRepositoryImpl
import com.google.refereeschedule.data.repository.ProfileRepositoryImpl
import com.google.refereeschedule.data.repository.SeasonRepositoryImpl
import com.google.refereeschedule.data.repository.UserRepositoryImpl
import com.google.refereeschedule.domain.repository.AssignmentRepository
import com.google.refereeschedule.domain.repository.GameRepository
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.SeasonRepository
import com.google.refereeschedule.domain.repository.UserRepository
import com.google.refereeschedule.data.repository.TeamRepositoryImpl
import com.google.refereeschedule.domain.repository.TeamRepository
import com.google.refereeschedule.data.repository.OrganizationRepositoryImpl
import com.google.refereeschedule.domain.repository.OrganizationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindGameRepository(
        gameRepositoryImpl: GameRepositoryImpl
    ): GameRepository

    @Binds
    @Singleton
    abstract fun bindSeasonRepository(
        seasonRepositoryImpl: SeasonRepositoryImpl
    ): SeasonRepository

    @Binds
    @Singleton
    abstract fun bindAssignmentRepository(
        assignmentRepositoryImpl: AssignmentRepositoryImpl
    ): AssignmentRepository

    @Binds
    @Singleton
    abstract fun bindTeamRepository(
        teamRepositoryImpl: TeamRepositoryImpl
    ): TeamRepository

    @Binds
    @Singleton
    abstract fun bindOrganizationRepository(
        organizationRepositoryImpl: OrganizationRepositoryImpl
    ): OrganizationRepository
}
