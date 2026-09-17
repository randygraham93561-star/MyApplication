package com.google.refereeschedule.di

import com.google.refereeschedule.data.repository.PrintRepositoryImpl
import com.google.refereeschedule.data.repository.PrintTemplateRepositoryImpl
import com.google.refereeschedule.data.repository.QuizRepositoryImpl
import com.google.refereeschedule.domain.repository.PrintRepository
import com.google.refereeschedule.domain.repository.PrintTemplateRepository
import com.google.refereeschedule.domain.repository.QuizRepository
import com.google.refereeschedule.data.repository.AnnouncementRepositoryImpl
import com.google.refereeschedule.data.repository.ChatRepositoryImpl
import com.google.refereeschedule.domain.repository.AnnouncementRepository
import com.google.refereeschedule.data.repository.StorageRepositoryImpl
import com.google.refereeschedule.domain.repository.ChatRepository
import com.google.refereeschedule.domain.repository.StorageRepository
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
    abstract fun bindAnnouncementRepository(
        announcementRepositoryImpl: AnnouncementRepositoryImpl
    ): AnnouncementRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: com.google.refereeschedule.data.repository.UserRepositoryImpl
    ): com.google.refereeschedule.domain.repository.UserRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        profileRepositoryImpl: com.google.refereeschedule.data.repository.ProfileRepositoryImpl
    ): com.google.refereeschedule.domain.repository.ProfileRepository

    @Binds
    @Singleton
    abstract fun bindGameRepository(
        gameRepositoryImpl: com.google.refereeschedule.data.repository.GameRepositoryImpl
    ): com.google.refereeschedule.domain.repository.GameRepository

    @Binds
    @Singleton
    abstract fun bindSeasonRepository(
        seasonRepositoryImpl: com.google.refereeschedule.data.repository.SeasonRepositoryImpl
    ): com.google.refereeschedule.domain.repository.SeasonRepository

    @Binds
    @Singleton
    abstract fun bindAssignmentRepository(
        assignmentRepositoryImpl: com.google.refereeschedule.data.repository.AssignmentRepositoryImpl
    ): com.google.refereeschedule.domain.repository.AssignmentRepository

    @Binds
    @Singleton
    abstract fun bindTeamRepository(
        teamRepositoryImpl: com.google.refereeschedule.data.repository.TeamRepositoryImpl
    ): com.google.refereeschedule.domain.repository.TeamRepository

    @Binds
    @Singleton
    abstract fun bindOrganizationRepository(
        organizationRepositoryImpl: com.google.refereeschedule.data.repository.OrganizationRepositoryImpl
    ): com.google.refereeschedule.domain.repository.OrganizationRepository

    @Binds
    @Singleton
    abstract fun bindQuizRepository(
        quizRepositoryImpl: QuizRepositoryImpl
    ): QuizRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindPrintRepository(
        printRepositoryImpl: PrintRepositoryImpl
    ): PrintRepository

    @Binds
    @Singleton
    abstract fun bindPrintTemplateRepository(
        printTemplateRepositoryImpl: PrintTemplateRepositoryImpl
    ): PrintTemplateRepository

    @Binds
    @Singleton
    abstract fun bindStorageRepository(
        storageRepositoryImpl: StorageRepositoryImpl
    ): StorageRepository
}
